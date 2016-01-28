#!/usr/bin/env python3
import json
import os
import subprocess

import docker as dckr
import signal
from datetime import datetime
from time import sleep
from benchmarks_impl import BenchmarksImpl


class Benchmarks:
    log_file_base = '/opt/erasuretester/results/result_'

    def __init__(self, docker, java):
        self.docker = docker
        self.java = java

        # Only 0 and the amount of Redis nodes configured in benchmark_in_docker.sh are allowed at the same time
        self.redis_size = [5, 0]
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
                            self.restart(ec, sb, ss, ps)
                            for b in self.benches:
                                self._run_benchmark(b, config)
            else:
                print("Cannot run benchmark on %d Redis nodes: not enough slaves" % rs)

    def _run_benchmark(self, bench, config):
        bench_name = bench.__name__
        print("    " + bench_name)
        self.results.setdefault(bench_name, {})[str(config)] = bench()

    def save_results_to_file(self):
        with open(self.log_file_base + datetime.today().isoformat() + '.json', 'w') as out:
            json.dump(self.results, out, indent=4)

    def trim_redis(self, cluster_size):
        if cluster_size <= 0:
            return True

        print("Trimming Redis to %d nodes" % cluster_size)
        slaves = self.get_redis_slaves()
        if len(slaves) + 1 < cluster_size:
            return False
        while len(slaves) + 1 > cluster_size:
            self.kill_a_redis_slave(slaves)

        self.fix_redis()
        return True

    def get_redis_slaves(self):
        return [x['Id'] for x in self.docker.containers(filters={
            'status': 'running',
            'label': 'com.docker.compose.service=redis-slave'
        })]

    def kill_a_redis_slave(self, slaves):
        victim = slaves.pop()
        self.docker.stop(victim)

    def restart(self, erasure, storage, stripe=None, parity=None, src=None, quiet=True):
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

        self.java.start(params)

    @staticmethod
    def fix_redis():
        print("Fixing Redis after trim")
        subprocess.call('echo yes | ruby ./redis-trib.rb fix %s' % os.environ['REDIS_ADDRESS'],
                        shell=True, stdout=subprocess.DEVNULL)


class JavaProgram:
    java_with_args = "java -cp * ch.unine.vauchers.erasuretester.Main /mnt/erasure".split(' ')

    def start(self, more_args):
        self.proc = subprocess.Popen(self.java_with_args + more_args)
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

if __name__ == '__main__':
    print("Python client ready, starting benchmarks")
    benchmarks = Benchmarks(dckr.Client('unix://var/run/docker.sock'), JavaProgram())
    benchmarks.run_benchmarks()
    print("Benchmarks ended, saving results to JSON file")
    benchmarks.save_results_to_file()
