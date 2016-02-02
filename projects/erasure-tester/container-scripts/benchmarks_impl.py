#!/usr/bin/env python3
import random
import re
import string
import subprocess


class BenchmarksImpl:
    """
    Collection of benchmarks to run against the filesystem. Each method defined in this class will be executed as a
    benchmark, provided that its name begins with bench_. Each method takes no parameters, and must return a dict. The
    dict contains the results of the execution in the following format:
    {
        'name of metric 1': (1234, 'kB/s'),
        'name of metric 2': (9876, 'writes/s')
    }
    """

    def __init__(self, mountpoint):
        self.mount = mountpoint

    def generate_file_name(self):
        return self.mount + ''.join(random.choice(string.ascii_letters) for _ in range(12))

    def bench_dd(self, config):
        results = {}
        for count in range(25, 75, 25):
            block_count = count * 20 if config[1] == 0 else count  # More data for the memory backend

            filename = self.generate_file_name()
            out = subprocess.check_output(("dd if=/dev/zero of=%s bs=4kB count=%d" % (filename, block_count))
                                                .split(' '), stderr=subprocess.STDOUT, universal_newlines=True)
            match = re.search(r'([0-9.]+) ([a-zA-Z]?B/s)$', out)
            results['%d kB write (4k blocks)' % (block_count * 4)] = (match.groups())

            out = subprocess.check_output(("dd if=%s of=/dev/null bs=4kB count=%d" % (filename, block_count))
                                               .split(' '), stderr=subprocess.STDOUT, universal_newlines=True)
            match = re.search(r'([0-9.]+) ([a-zA-Z]?B/s)$', out)
            results['%d kB read (4k blocks)' % (block_count * 4)] = (match.groups())

        return results
