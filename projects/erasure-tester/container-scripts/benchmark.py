#!/usr/bin/env python3
import json
import logging
import subprocess
from datetime import datetime

import pydevd as pydevd
import signal
import sys
from time import sleep

from benchmarks_impl import BenchmarksImpl
from nodes_trace import NodesTrace
from redis_cluster import RedisCluster
from utils import kill_pid


class Benchmarks:
    log_file_base = '/opt/erasuretester/results/result_'

    def __init__(self):
        # 2 is forbidden due to Redis limitation on Cluster size
        self.redis_trace_configs = [
            {'synthetic': list(range(15, 2, -1))},
            {'synthetic': list(range(60, 2, -1))},
            {'synthetic': list(range(100, 2, -1))}
        ]
        self.erasure_codes = ['Null', 'ReedSolomon', 'SimpleRegenerating']
        self.erasure_configs = {
            'ReedSolomon': [
                (10, 4, 0)
            ],
            'SimpleRegenerating': [
                (10, 6, 5)
            ],
            'Null': [
                (10, 0, 0)
            ]
        }
        self.first = True
        self.results = []
        self.log_file_base += datetime.today().isoformat()

        benchmarks_impl = BenchmarksImpl('/mnt/erasure/')
        self.benches = [getattr(benchmarks_impl, m) for m in dir(benchmarks_impl) if m.startswith('bench_')]

    def run_benchmarks(self):
        for nodes_trace_config in self.redis_trace_configs:
            for erasure_code in self.erasure_codes:
                for (stripe_size, parity_size, src) in self.erasure_configs[erasure_code]:
                    for b in self.benches:
                        nodes_trace = NodesTrace(**nodes_trace_config)
                        initial_redis_size = nodes_trace.initial_size()

                        with RedisCluster(initial_redis_size) as redis:
                            sb = 'Jedis' if initial_redis_size > 0 else 'Memory'
                            config = [erasure_code, initial_redis_size, sb, stripe_size, parity_size, src]
                            print("Running with " + str(config))
                            (params, env) = self._get_java_params(redis, *config)
                            with JavaProgram(params, env) as java:
                                try:
                                    self._run_benchmark(b, config, redis, java, nodes_trace)
                                except Exception as ex:
                                    logging.exception("The benchmark crashed, continuing with the rest...")
                        self.save_results_to_file()

    def _run_benchmark(self, bench, config, redis, java, nodes_trace):
        bench_name = bench.__name__
        print("    " + bench_name)
        self.results.append({
            'bench': bench_name,
            'config': config,
            'results': bench(config, redis, java, nodes_trace)
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
    java_with_args = ["java"]
    # java_with_args += ["-agentlib:jdwp=transport=dt_socket,server=n,address=172.16.0.167:5005,suspend=y"]
    java_with_args += "-Xmx6G -cp * ch.unine.vauchers.erasuretester.Main /mnt/erasure".split(' ')
    proc = None

    def __init__(self, more_args, env):
        self.more_args = more_args
        self.env = env

    def __enter__(self):
        self.proc = subprocess.Popen(self.java_with_args + self.more_args, env=self.env, stdout=subprocess.PIPE,
                                     stderr=subprocess.STDOUT, stdin=subprocess.PIPE)
        sleep(10)
        return self

    def __exit__(self, exc_type, exc_val, exc_tb):
        kill_pid(self.proc)

    def flush_read_cache(self):
        self.proc.stdin.write(b"clearCache\n")
        self._wait_command_completion()

    def repair_all_files(self):
        self.proc.stdin.write(b"repairAll\n")
        self._wait_command_completion()

    def repair_file(self, filepath):
        self.proc.stdin.write(("repair %s\n" % filepath).encode())
        self._wait_command_completion()

    def _wait_command_completion(self):
        self.proc.stdout.flush()
        self.proc.stdin.flush()
        limit = 3
        while limit > 0 and self.proc.stdout.readline() != b"Done\n":
            limit -= 1
            print("Waiting for the command to complete...")
        if limit <= 0:
            print("The command did not complete!")


if __name__ == '__main__':
    # pydevd.settrace('172.16.0.167', port=9292, stdoutToServer=True, stderrToServer=True)
    print("Python client ready, starting benchmarks")
    benchmarks = Benchmarks()


    def signal_handler(signal, frame):
        benchmarks.save_results_to_file()
        sys.exit(0)


    signal.signal(signal.SIGTERM, signal_handler)
    signal.signal(signal.SIGINT, signal_handler)

    try:
        benchmarks.run_benchmarks()
    except Exception as ex:
        logging.exception("Something crashed")
    finally:
        print("Benchmarks ended, saving results to JSON file")
        benchmarks.save_results_to_file()
