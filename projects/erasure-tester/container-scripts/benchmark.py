#!/usr/bin/env python3
import json
import logging
import os
import re
import socket
import subprocess
from datetime import datetime

import signal
import sys
from benchmarks_impl import BenchmarksImpl
from plot_results import main as output_plots
from redis_cluster import RedisCluster
from time import sleep


class Benchmarks:
    log_file_base = '/opt/erasuretester/results/result_'

    def __init__(self):
        # 2 is forbidden due to Redis limitation on Cluster size
        self.redis_size = [5, 1, 0]
        self.erasure_codes = ['Null']
        self.stripe_sizes = [10]
        self.parity_sizes = [2]
        self.src_sizes = [5]
        self.first = True
        self.results = []
        self.log_file_base += datetime.today().isoformat()

        benchmarks_impl = BenchmarksImpl('/mnt/erasure/')
        self.benches = [getattr(benchmarks_impl, m) for m in dir(benchmarks_impl) if m.startswith('bench_')]

    def run_benchmarks(self):
        for rs in self.redis_size:
            for ec in self.erasure_codes:
                for ss in self.stripe_sizes:
                    for ps in self.parity_sizes:
                        for src in self.src_sizes:
                            for b in self.benches:
                                with RedisCluster(rs) as redis:
                                    sb = 'Jedis' if rs > 0 else 'Memory'
                                    config = [ec, rs, sb, ss, ps, src]
                                    print("Running with " + str(config))
                                    (params, env) = self._get_java_params(redis, *config)
                                    with JavaProgram(params, env):
                                        try:
                                            self._run_benchmark(b, config)
                                        except Exception as ex:
                                            logging.exception("The benchmark crashed, continuing with the rest...")

    def _run_benchmark(self, bench, config):
        bench_name = bench.__name__
        print("    " + bench_name)
        self.results.append({
            'bench': bench_name,
            'config': config,
            'results': bench(config)
        })

    def save_results_to_file(self):
        with open(self.log_file_base + '.json', 'w') as out:
            json.dump(self.results, out, indent=4)

    @staticmethod
    def _get_java_params(redis, erasure, redis_size, storage, stripe=None, parity=None, src=None, quiet=True):
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

        env = {'REDIS_ADDRESS': redis.get_master_node_str(redis_size)} if redis_size > 0 else {}
        return params, env


class JavaProgram:
    java_with_args = "java -cp * ch.unine.vauchers.erasuretester.Main /mnt/erasure".split(' ')
    proc = None

    def __init__(self, more_args, env):
        self.more_args = more_args
        self.env = env

    def __enter__(self):
        self.proc = subprocess.Popen(self.java_with_args + self.more_args, env=self.env)
        sleep(10)
        return self

    def __exit__(self, exc_type, exc_val, exc_tb):
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
    sleep(10)
    print("Python client ready, starting benchmarks")
    benchmarks = Benchmarks()

    def signal_handler(signal, frame):
        benchmarks.save_results_to_file()
        sys.exit(0)
    signal.signal(signal.SIGTERM, signal_handler)
    signal.signal(signal.SIGINT, signal_handler)

    try:
        benchmarks.run_benchmarks()
    finally:
        print("Benchmarks ended, saving results to JSON file")
        benchmarks.save_results_to_file()
        output_plots(benchmarks.log_file_base + '.pdf')
