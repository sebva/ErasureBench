#!/usr/bin/env python3
import os
import random
import string
import subprocess

import docker as dckr
import signal
from time import sleep


class Benchmarks:
    def __init__(self, docker, java):
        self.docker = docker
        self.java = java

        self.redis_size = [5, 3, 1, 0]
        self.erasure_codes = ['Null']
        self.stripe_sizes = [10]
        self.parity_sizes = [0]
        self.benches = [self.dd_write]
        self.first = True

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
                                b()

    def trim_redis(self, cluster_size):
        print("Trimming Redis to %d nodes" % cluster_size)
        if cluster_size <= 0:
            return True

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

    def dd_write(self):
        subprocess.Popen(("dd if=/dev/zero of=/mnt/erasure/%s bs=4096 count=10"
                          % self.generate_file_name()).split(' ')).wait()

    @staticmethod
    def generate_file_name():
        return ''.join(random.choice(string.ascii_letters) for _ in range(12))

    def restart(self, erasure, storage, stripe=None, parity=None, src=None, quiet=True):
        if self.first:
            self.first = False
        else:
            self.java.kill()

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
        subprocess.Popen('echo yes | ruby ./redis-trib.rb fix %s' % os.environ['REDIS_ADDRESS'], shell=True).wait()


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
    Benchmarks(dckr.Client('unix://var/run/docker.sock'), JavaProgram()).run_benchmarks()
