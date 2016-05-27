import http.server
import os
import random
import socket
import subprocess

import re
import threading
from http.server import BaseHTTPRequestHandler, HTTPServer
from time import sleep

import docker
import requests


def florida_string():
    ps_output = subprocess.check_output('docker-compose ps redis-master', shell=True).decode().splitlines()
    ps_output_q = subprocess.check_output('docker-compose ps -q redis-master', shell=True).decode().splitlines()

    nodes_names = [x.split(' ')[0] for x in ps_output if x.startswith('erasuretester')]
    nodes_tokens = [str(int(x[:10], 16)) for x in ps_output_q]

    florida = [x[0] + ":8101:rack1:dc:" + x[1] for x in zip(nodes_names, nodes_tokens)]
    return '|'.join(florida).encode()


class FloridaHandler(BaseHTTPRequestHandler):
    def do_GET(self):
        if self.path == '/REST/v1/admin/get_seeds':
            self.send_response(200)
            self.send_header("Content-type", "text/plain")
            self.end_headers()
            self.wfile.write(florida_string())
        else:
            self.send_response(404)


class RedisCluster:
    def __init__(self, cluster_size=3):
        self.real_cluster_size = 0
        self.cluster_size = cluster_size
        self.server = HTTPServer(('', 4321), FloridaHandler)
        self.server_thread = threading.Thread(target=self.server.serve_forever)
        self.server_thread.daemon = True

    def __enter__(self):
        print("Starting Florida server")
        self.server_thread.start()
        print("Starting a Dynomite cluster of %d nodes" % self.cluster_size)
        try:
            self.dckr = docker.Client(base_url=os.environ['DOCKER_HOST'])
        except KeyError:
            self.dckr = docker.Client()
        if self.cluster_size == 0:
            print("Nothing to do")
            return self
        elif self.cluster_size == 1:
            self._docker_scale(1, standalone=True)
            sleep(5)
            return self

        attempt = 0
        success = False
        while not success:
            attempt += 1

            # Ramp up by batches of 30, otherwise we get a timeout exception
            current = 0
            remaining = self.cluster_size
            while remaining > 0:
                to_add = 30 if remaining > 30 else remaining
                current += to_add
                remaining -= to_add
                self._docker_scale(current)

            print("Waiting for all nodes to come alive...")
            sleep(5)
            primitive_nodes = self._get_nodes()

            if len(primitive_nodes) < self.cluster_size:
                if attempt > 3:
                    raise Exception("Too much unsuccessful attempts at starting the Redis cluster")
                else:
                    print("There has been a problem, trying again...")
                    self._docker_scale(0)
            else:
                success = True

        self._start_cluster()
        return self

    def __exit__(self, exc_type, exc_val, exc_tb):
        self.server.shutdown()
        if self.cluster_size == 0:
            return
        print("Killing the Dynomite cluster")
        self._docker_scale(0, standalone=self.cluster_size == 1)
        sleep(2)

    def scale(self, new_cluster_size):
        """
        Scale the cluster up or down to the specified amount of nodes
        :param new_cluster_size: The new number of nodes desired
        :return: True if successful
        """
        if new_cluster_size <= 1:
            return True

        print("Scaling Redis to %d nodes" % new_cluster_size)
        self._docker_scale(new_cluster_size)

        self.cluster_size = new_cluster_size
        self._start_cluster()
        return self.real_cluster_size == self.cluster_size

    def get_master_node_str(self, redis_size):
        if redis_size == 1:
            return ':'.join(map(str, socket.getaddrinfo('erasuretester_redis-standalone_1', 6379, socket.AF_INET)[0][4]))
        else:
            return ':'.join(map(str, self._get_nodes()[0]))

    @staticmethod
    def _docker_scale(cluster_size, standalone=False):
        node_type = "standalone" if standalone else "master"
        subprocess.check_call('docker-compose scale -t 2 redis-%s=%d' % (node_type, cluster_size), shell=True)

    def _get_nodes(self):
        container_ids = subprocess.check_output('docker-compose ps -q redis-master', shell=True).decode().splitlines()
        return [
            (self.dckr.inspect_container(c)['NetworkSettings']['Networks']['erasuretester_default']['IPAddress'], 8102)
            for c in container_ids]

    def _start_cluster(self):
        if self.cluster_size <= 1:
            return

        ps_output = []
        while len(ps_output) == 0:
            sleep(1)
            ps_output = subprocess.check_output('docker-compose ps -q redis-master', shell=True).decode().splitlines()

        node_1_id = ps_output[0]
        node_1_ip_port = self.dckr.inspect_container(node_1_id)['NetworkSettings']['Networks']['erasuretester_default']['IPAddress'] + ':22222'
        retries = 120
        last_report = -1
        while retries > 0 and self.real_cluster_size < self.cluster_size:
            sleep(1)
            self.real_cluster_size = requests.get('http://%s/cluster_describe' % node_1_ip_port).text.count('erasuretester')
            if last_report != self.real_cluster_size:
                last_report = self.real_cluster_size
                print("Dynomite discovery: %d/%d" % (self.real_cluster_size, self.cluster_size))
            retries -= 1
        if retries <= 0:
            print("Dynomite discovery failed!")
