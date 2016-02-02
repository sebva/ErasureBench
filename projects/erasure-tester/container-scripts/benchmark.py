#!/usr/bin/env python3
import json
import os
import re
import socket
import subprocess
from datetime import datetime

import signal
from benchmarks_impl import BenchmarksImpl
from time import sleep


class Benchmarks:
    log_file_base = '/opt/erasuretester/results/result_'

    def __init__(self, java):
        self.java = java

        # 2 is forbidden due to Redis limitation on Cluster size
        self.redis_size = [5, 3, 1, 0]
        self.erasure_codes = ['Null']
        self.stripe_sizes = [10]
        self.parity_sizes = [0]
        self.first = True
        self.results = {}

        benchmarks_impl = BenchmarksImpl('/mnt/erasure/')
        self.benches = [getattr(benchmarks_impl, m) for m in dir(benchmarks_impl) if m.startswith('bench_')]

    def run_benchmarks(self):
        for rs in self.redis_size:
            if self.trim_redis(rs):
                for ec in self.erasure_codes:
                    for ss in self.stripe_sizes:
                        for ps in self.parity_sizes:
                            sb = 'Jedis' if rs > 0 else 'Memory'
                            config = (ec, rs, sb, ss, ps)
                            print("Running with " + str(config))
                            self.restart(*config)
                            for b in self.benches:
                                self._run_benchmark(b, config)
            else:
                print("Cannot run benchmark on %d Redis nodes: not enough slaves" % rs)

    def _run_benchmark(self, bench, config):
        bench_name = bench.__name__
        print("    " + bench_name)
        self.results.setdefault(bench_name, {})[str(config)] = bench(config)

    def save_results_to_file(self):
        with open(self.log_file_base + datetime.today().isoformat() + '.json', 'w') as out:
            json.dump(self.results, out, indent=4)

    def trim_redis(self, cluster_size, brutal=False):
        if cluster_size <= 1:
            return True

        print("Trimming Redis to %d nodes" % cluster_size)
        nodes = self.get_redis_masters()
        if len(nodes) < cluster_size:
            return False
        while len(nodes) > cluster_size:
            if brutal:
                self.kill_a_redis_node(nodes)
            else:
                self.remove_a_redis_node(nodes)

        return True

    def get_redis_masters(self):
        nodes = [x.split(' ') for x in
                 subprocess.check_output('redis-cli -h 172.18.0.2 CLUSTER NODES'.split(' ')).decode(
                         'UTF-8').splitlines()]
        return [{
                    'id': x[0],
                    'ip_port': x[1],
                    'is_number_1': 'myself' in x[2]
                } for x in nodes]

    def elect_redis_victim(self, nodes):
        victim = None
        nodes_iter = iter(nodes)
        while victim is None:
            victim = next(nodes_iter)
            if victim['is_number_1']:
                victim = None
        nodes.remove(victim)
        return victim

    def kill_a_redis_node(self, nodes):
        victim = self.elect_redis_victim(nodes)

        # Brutally kill the node
        subprocess.check_call(['redis-cli', '-h', victim['ip_port'].split(':')[0], 'SHUTDOWN'])
        for node in nodes:
            subprocess.check_call(['redis-cli', '-h', node['ip_port'].split(':')[0], 'CLUSTER', 'FORGET', victim['id']])

        redistrib = subprocess.Popen(['ruby', 'redis-trib.rb', 'fix', nodes[0]['ip_port']], stdin=subprocess.PIPE, stdout=subprocess.DEVNULL)
        redistrib.communicate(b'yes\n')
        redistrib.wait()

    def remove_a_redis_node(self, nodes):
        victim = self.elect_redis_victim(nodes)
        master_ip_port = [x['ip_port'] for x in nodes if x['is_number_1']][0]
        info = subprocess.check_output(['ruby', 'redis-trib.rb', 'info', master_ip_port]).decode('UTF-8').splitlines()
        slots_to_remove = int(
                re.search(r'([0-9]+) slots', [x for x in info if x.startswith(victim['ip_port'])][0]).group(1))
        slots_to_remove_per_node = int(slots_to_remove / len(nodes))
        slots_to_remove -= slots_to_remove_per_node * len(nodes)

        for node in nodes:
            self.transfer_slots(victim['id'], node['id'], slots_to_remove_per_node, master_ip_port)
        if slots_to_remove > 0:
            self.transfer_slots(victim['id'], nodes[0]['id'], slots_to_remove, master_ip_port)

        subprocess.check_call(['ruby', 'redis-trib.rb', 'del-node', master_ip_port, victim['id']])

    def transfer_slots(self, from_id, to_id, amount, master_ip_port):
        subprocess.check_call(('ruby redis-trib.rb reshard --from %s --to %s --slots %d --yes %s' % (
            from_id, to_id, amount, master_ip_port)).split(' '), stdout=subprocess.DEVNULL)

    def restart(self, erasure, redis_size, storage, stripe=None, parity=None, src=None, quiet=True):
        if self.first:
            self.first = False
        else:
            self.java.kill()
            sleep(1)

        params = [
            '--erasure-code', erasure,
            '--storage', storage
        ]
        if quiet:
            params += ['-q']
        if stripe is not None:
            params += ['--stripe', str(stripe)]
        if parity is not None:
            params += ['--parity', str(parity)]
        if src is not None:
            params += ['--src', str(src)]
        if redis_size > 1:
            params += ['--redis-cluster']

        env = {'REDIS_ADDRESS': get_redis_node_str(redis_size)}
        self.java.start(params, env)


class JavaProgram:
    java_with_args = "java -cp * ch.unine.vauchers.erasuretester.Main /mnt/erasure".split(' ')

    def start(self, more_args, env):
        self.proc = subprocess.Popen(self.java_with_args + more_args, env=env)
        sleep(10)

    def kill(self):
        kill_pid(self.proc)


def kill_pid(proc):
    """
    Kill a process. Try SIGTERM first, then SIGKILL
    :type proc: subprocess.Popen
    """
    print("Terminating process %d" % proc.pid)
    os.kill(proc.pid, signal.SIGTERM)
    timeout = 10
    while timeout > 0:
        sleep(1)
        timeout -= 1
        if proc.poll() is not None:
            timeout = 0

    if proc.poll() is None:
        print("Process %d still alive, using SIGKILL" % proc.pid)
        os.kill(proc.pid, signal.SIGKILL)
        proc.wait()


def start_redis_cluster():
    args = ['ruby', 'redis-trib.rb', 'create']

    args += [':'.join(map(str, x)) for x in get_redis_nodes()]

    redistrib = subprocess.Popen(args, stdin=subprocess.PIPE)
    redistrib.communicate(b'yes\n')
    redistrib.wait()


def get_redis_nodes():
    redis_nodes = []
    try:
        i = 1
        while True:
            redis_nodes.append(socket.getaddrinfo('erasuretester_redis-master_%d' % i, 6379, socket.AF_INET)[0][4])
            i += 1
    except socket.gaierror:
        pass
    return redis_nodes


def get_redis_node_str(redis_size):
    container_name = 'standalone' if redis_size <= 1 else 'master'
    return ':'.join(
            map(str, socket.getaddrinfo('erasuretester_redis-%s_1' % container_name, 6379, socket.AF_INET)[0][4]))


if __name__ == '__main__':
    sleep(10)
    print("Configuring Redis cluster")
    start_redis_cluster()
    print("Python client ready, starting benchmarks")
    benchmarks = Benchmarks(JavaProgram())
    benchmarks.run_benchmarks()
    print("Benchmarks ended, saving results to JSON file")
    benchmarks.save_results_to_file()
