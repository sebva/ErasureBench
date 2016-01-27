#!/usr/bin/env python3
import random
import string

import Pyro4
import docker as dckr
import requests
import subprocess
import time


class Benchmarks:
    def __init__(self, erasure_server, docker):
        self.erasure_server = erasure_server
        self.docker = docker

        self.redis_size = [5, 3, 1]
        self.storage_backends = ['Memory', 'Jedis']
        self.erasure_codes = ['Null']
        self.stripe_sizes = [10]
        self.parity_sizes = [0]
        self.benches = [self.dd_write]

    def run_benchmarks(self):
        for rs in self.redis_size:
            if self.trim_redis(rs):
                for ec in self.erasure_codes:
                    for sb in self.storage_backends:
                        for ss in self.stripe_sizes:
                            for ps in self.parity_sizes:
                                config = (ec, sb, ss, ps)
                                print("Running with " + str(config))
                                restart(self.erasure_server, ec, sb, ss, ps)
                                for b in self.benches:
                                    b()
                        
    def trim_redis(self, cluster_size):
        print("Trimming Redis to %d nodes" % cluster_size)
        slaves = self.get_redis_slaves()
        if len(slaves) + 1 < cluster_size:
            return False
        while len(slaves) + 1 > cluster_size:
            self.kill_a_redis_slave(slaves)

        self.erasure_server.fix_redis()
        return True

    def get_redis_slaves(self):
        return [x['Id'] for x in self.docker.containers(filters={
            'status': 'running',
            'label': 'com.docker.compose.service=redis-slave'
        })]

    def kill_a_redis_slave(self, slaves):
        victim = slaves.pop()
        self.docker.stop(victim)

    def dd_write(self):
        subprocess.Popen(("dd if=/dev/zero of=/mnt/erasure/%s bs=4096 count=10"
                          % self.generate_file_name()).split(' ')).wait()

    @staticmethod
    def generate_file_name():
        return ''.join(random.choice(string.ascii_letters) for _ in range(12))


def restart(server, erasure, storage, stripe=None, parity=None, src=None, quiet=True):
    server.kill()
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
    server.start(params)


def wait_for_server(server):
    print("Waiting for the server to be ready")
    server_ready = False
    while not server_ready:
        try:
            server_ready = server.ping()
        except (Pyro4.errors.CommunicationError, requests.exceptions.ConnectionError):
            server_ready = False
            time.sleep(3)


if __name__ == '__main__':
    print("Starting Python client")
    uri = Pyro4.URI("PYRO:benchmarkserver@erasuretester_erasure_1:9999")
    benchmark_server = Pyro4.Proxy(uri)
    wait_for_server(benchmark_server)
    docker_api = dckr.Client('unix://var/run/docker.sock')

    print("Python client ready, starting benchmarks")
    Benchmarks(benchmark_server, docker_api).run_benchmarks()
