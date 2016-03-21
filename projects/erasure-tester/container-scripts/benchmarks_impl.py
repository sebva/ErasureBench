#!/usr/bin/env python3
import random
import re
import string
import subprocess

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
