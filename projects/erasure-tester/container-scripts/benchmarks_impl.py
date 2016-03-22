#!/usr/bin/env python3
import random
import re
import string
import subprocess

import sys
from redis_cluster import RedisCluster


class BenchmarksImpl:
    """
    Collection of benchmarks to run against the filesystem. Each method defined in this class will be executed as a
    benchmark, provided that its name begins with bench_. Each method takes a single parameter in the form of a tuple.
    It contains the configuration in which the test is running, like this:
    (Erasure code name, Number of nodes in the Redis cluster, Storage backend name, Stripe size, Parity size, SRC size)

    Each bench_ method must return a dict formed like the following:
    {
        'name of metric 1': 1234,
        'name of metric 2': 9876,
    }
    """

    def __init__(self, mountpoint):
        self.mount = mountpoint

    def generate_file_name(self):
        return self.mount + ''.join(random.choice(string.ascii_letters) for _ in range(12))

    @staticmethod
    def _convert_to_kb(value, unit):
        if unit.startswith('M'):
            return float(value) * 1000.0
        elif unit.startswith('k'):
            return float(value)
        else:
            raise Exception('Unit not supported, please complete the _convert_to_kb method')

    def bench_apache(self, config, redis: RedisCluster, java):
        tar_log_lines = 2614
        sha_log_lines = 2517

        print('Uncompressing httpd...')
        tar_proc = subprocess.Popen(['tar', '-xvjf', '/opt/erasuretester/httpd.tar.bz2', '-C', self.mount], stdout=subprocess.PIPE, bufsize=1)
        self._show_subprocess_percent(tar_proc, tar_log_lines)

        results = dict()
        while redis.cluster_size >= 2:
            print('Checking files...')
            sha_proc = subprocess.Popen(
                ['sha256sum', '-c', '/opt/erasuretester/httpd.sha256'],
                stdout=subprocess.PIPE, bufsize=1)
            sha_output = self._show_subprocess_percent(sha_proc, sha_log_lines)
            ok_files = len([x for x in sha_output if b' OK' in x])
            failed_files = len([x for x in sha_output if b' FAILED' in x])
            print('   Checked. %d correct, %d failed' % (ok_files, failed_files))
            inter_results = {
                'RS0': config[1],
                'RS': redis.cluster_size,
                'Failure ratio': failed_files / (ok_files + failed_files),
            }
            results['RS=%d' % redis.cluster_size] = inter_results

            if ok_files == 0:  # It's no use to continue
                break
            if redis.cluster_size > 2:
                redis.scale(redis.cluster_size - 1, brutal=True)
                print('Flushing read cache...')
                java.flush_read_cache()
        return results

    @staticmethod
    def _show_subprocess_percent(proc, expected_nb_lines):
        percent = 0
        lines = []
        with proc.stdout:
            for line in iter(proc.stdout.readline, b''):
                lines.append(line)
                new_percent = int(len(lines) / expected_nb_lines * 100)
                if new_percent != percent and new_percent % 5 == 0:
                    percent = new_percent
                    print("%d %%" % percent)
        proc.wait()
        return lines

    def bench_kill(self, config, redis: RedisCluster, java):
        if config[1] < 2 or config[0] == 'Null':
            # The benchmark would crash needlessly
            return {}

        self.bench_dd(block_count=20)
        redis.scale(redis.cluster_size - 1, brutal=True)
        java.flush_read_cache()
        self.bench_dd(block_count=20)
        return {}

    def bench_dd(self, config=None, redis=None, java=None, block_count=50):
        write_speed = read_speed = 0

        for _ in range(3):
            filename = self.generate_file_name()
            out = subprocess.check_output(("dd if=/dev/zero of=%s bs=128kB count=%d" % (filename, block_count))
                                          .split(' '), stderr=subprocess.STDOUT, universal_newlines=True)
            match = re.search(r'([0-9.]+) ([a-zA-Z]?B/s)$', out)
            write_speed = max(self._convert_to_kb(*match.groups()), write_speed)

            out = subprocess.check_output(("dd if=%s of=/dev/null bs=128kB count=%d" % (filename, block_count))
                                          .split(' '), stderr=subprocess.STDOUT, universal_newlines=True)
            match = re.search(r'([0-9.]+) ([a-zA-Z]?B/s)$', out)
            read_speed = max(self._convert_to_kb(*match.groups()), read_speed)

        return {
            'read': read_speed,
            'write': write_speed
        }
