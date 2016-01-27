#!/usr/bin/env python3
import os
import subprocess

import Pyro4
import signal
from time import sleep


java_with_args = "java -cp * ch.unine.vauchers.erasuretester.Main /mnt/erasure".split(' ')


class BenchmarkServer:
    def __init__(self):
        self.start([])

    def kill(self):
        kill_pid(self.proc)

    def start(self, more_args):
        self.proc = subprocess.Popen(java_with_args + more_args)
        sleep(10)

    def fix_redis(self):
        pass

    def ping(self):
        return True


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


daemon = Pyro4.Daemon("0.0.0.0", 9999)
uri = daemon.register(BenchmarkServer(), "benchmarkserver")
daemon.requestLoop()
