#!/usr/bin/env python3
import random
import string
import subprocess

import re


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

    def bench_dd_write(self):
        out = subprocess.check_output(("dd if=/dev/zero of=%s bs=4096 count=25" % self.generate_file_name())
                                      .split(' '), stderr=subprocess.STDOUT, universal_newlines=True)
        match = re.search(r'([0-9.]+) ([a-zA-Z]?B/s)$', out)
        return {'100 kB write (4k blocks)': (match.groups())}
