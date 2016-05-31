import os
import random
import re
import socket
import subprocess
from time import sleep

import docker
from redis.client import Redis


class RedisCluster:
    nodes = []

    def __init__(self, cluster_size=3):
        self.cluster_size = cluster_size

    def __enter__(self):
        print("Starting a Redis cluster of %d nodes" % self.cluster_size)
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
            primitive_nodes = self._get_nodes_primitive()

            if len(primitive_nodes) < self.cluster_size:
                if attempt > 3:
                    raise Exception("Too much unsuccessful attempts at starting the Redis cluster")
                else:
                    print("There has been a problem, trying again...")
                    self._docker_scale(0)
            else:
                success = True

        self._start_cluster()
        self.nodes = self._get_running_nodes()
        return self

    def __exit__(self, exc_type, exc_val, exc_tb):
        if self.cluster_size == 0:
            return
        print("Killing the Redis cluster")
        self._docker_scale(0, standalone=self.cluster_size == 1)
        sleep(2)

    def scale(self, new_cluster_size, brutal=False):
        """
        Scale the cluster up or down to the specified amount of nodes
        :param new_cluster_size: The new number of nodes desired
        :param brutal: If there are nodes to be killed, whether it has to be done brutally (true) or gracefully (false)
        :return: True if successful
        """
        if new_cluster_size <= 1:
            return True

        print("Scaling Redis to %d nodes" % new_cluster_size)
        if self.cluster_size < new_cluster_size:
            self._add_new_nodes(new_cluster_size)
        else:
            while len(self.nodes) > new_cluster_size:
                if brutal:
                    self._kill_a_node()
                else:
                    self._remove_a_node()

        self.cluster_size = new_cluster_size
        self.nodes = self._get_running_nodes()
        return len(self.nodes) == self.cluster_size

    def get_master_node_str(self, redis_size):
        if redis_size == 1:
            return ':'.join(map(str, socket.getaddrinfo('erasuretester_redis-standalone_1', 6379, socket.AF_INET)[0][4]))
        container_id = subprocess.check_output('docker-compose ps -q redis-master | head -n 1', shell=True).decode().rstrip()
        return self.dckr.inspect_container(container_id)['NetworkSettings']['Networks']['erasuretester_default']['IPAddress'] + ":6379"

    def flushall(self):
        if self.cluster_size == 1:
            subprocess.check_call(['redis-cli', '-h', 'erasuretester_redis-standalone_1', 'FLUSHALL'])
        elif self.cluster_size >= 2:
            nodes = self._get_running_nodes()
            for node in nodes:
                subprocess.check_call(['redis-cli', '-h', node['ip_port'].split(':')[0], 'FLUSHALL'])

    def _add_new_nodes(self, cluster_size):
        old_nodes = self.nodes.copy()
        nodes_before = self._get_nodes_primitive()
        self._docker_scale(cluster_size)
        nodes_after = self._get_nodes_primitive()

        new_ips = [':'.join(map(str, x)) for x in set(nodes_after) - set(nodes_before)]
        print(new_ips)
        master_ip_port = old_nodes[0]['ip_port']
        master_ip, master_port = master_ip_port.split(':')
        master_conn = Redis(master_ip, master_port)

        print("Adding nodes to the cluster")
        for ip in new_ips:
            new_ip, new_port = ip.split(':')
            master_conn.execute_command('CLUSTER MEET', new_ip, new_port)

        print("Preventive fix")
        sleep(3)
        fix = subprocess.Popen(['ruby', 'redis-trib.rb', 'fix', master_ip_port], stdin=subprocess.PIPE, stdout=subprocess.DEVNULL)
        fix.communicate(b'yes\n')
        fix.wait()
        sleep(3)

        new_nodes = [x for x in self._get_running_nodes() if x['ip_port'] in new_ips]
        slots_per_node = round(16384 / cluster_size)

        old_redises = {x[0]: Redis(x[0], x[1]) for x in (y['ip_port'].split(':') for y in old_nodes)}
        new_redises = [Redis(x[0], x[1]) for x in (y['ip_port'].split(':') for y in new_nodes)]
        slots_repartition = self._get_slots_repartition(list(old_redises.values())[0])

        for dest_node, dest_redis, i in zip(new_nodes, new_redises, range(len(new_nodes))):
            slots = slots_repartition[i * slots_per_node: (i + 1) * slots_per_node]
            sources_ip = {x[1] for x in slots}
            for source_ip in sources_ip:
                slots_for_source = [x for x in slots if x[1] == source_ip]
                source_redis = old_redises[source_ip]
                self._transfer_slots(source_redis, slots_for_source[0][3],
                                     dest_redis, dest_node['id'],
                                     [x[0] for x in slots_for_source])

        subprocess.check_call(['ruby', 'redis-trib.rb', 'info', master_ip_port])

    @staticmethod
    def _get_slots_repartition(any_redis_conn: Redis):
        """
        Returns a shuffled list of (slot_number, node_ip, node_port, node_id)
        """
        # List of [10923, 16383, [b'10.0.0.4', 6379, b'f1dc21d0b7a24aaea3b3fcd0ef943a35fa2ebb42']]
        cluster_slots = any_redis_conn.execute_command('CLUSTER SLOTS')
        output = []
        for slot in cluster_slots:
            for i in range(slot[0], slot[1] + 1):
                output.append((i, slot[2][0].decode(), slot[2][1], slot[2][2].decode()))
        random.shuffle(output)
        return output

    @staticmethod
    def _transfer_slots(redis_conn_from: Redis, redis_id_from: str, redis_conn_to: Redis, redis_id_to: str, slots: list):
        """
        Documentation from http://redis.io/commands/cluster-setslot
        1. Set the destination node slot to importing state using CLUSTER SETSLOT <slot> IMPORTING <source-node-id>.
        2. Set the source node slot to migrating state using CLUSTER SETSLOT <slot> MIGRATING <destination-node-id>.
        3. Get keys from the source node with CLUSTER GETKEYSINSLOT command and move them into the destination node
           using the MIGRATE command.
        4. Use CLUSTER SETSLOT <slot> NODE <destination-node-id> in the source or destination.
        """
        print('Transfering %d slots from %s to %s...' % (len(slots), redis_id_from, redis_id_to))
        dest_host = redis_conn_to.connection_pool.connection_kwargs['host']
        dest_port = redis_conn_to.connection_pool.connection_kwargs['port']

        pipeline_to = redis_conn_to.pipeline()
        pipeline_from = redis_conn_from.pipeline()
        for slot in slots:
            # 1, 2
            pipeline_to.execute_command('CLUSTER SETSLOT', slot, 'IMPORTING', redis_id_from)
            pipeline_from.execute_command('CLUSTER SETSLOT', slot, 'MIGRATING', redis_id_to)
        pipeline_to.execute()
        pipeline_from.execute()

        for slot in slots:
            # 3
            keys = redis_conn_from.execute_command('CLUSTER GETKEYSINSLOT', slot, 1000000)
            if len(keys) > 0:
                redis_conn_from.execute_command('MIGRATE', dest_host, dest_port, "", 0, 180000, 'KEYS', *keys)
            # 4
            redis_conn_to.execute_command('CLUSTER SETSLOT', slot, 'NODE', redis_id_to)

    @staticmethod
    def _docker_scale(cluster_size, standalone=False):
        node_type = "standalone" if standalone else "master"
        subprocess.check_call('docker-compose scale redis-%s=%d' % (node_type, cluster_size), shell=True)
        sleep(3)

    @staticmethod
    def _get_running_nodes():
        nodes = [x.split(' ') for x in
                 subprocess.check_output('redis-cli -h erasuretester_redis-master_1 CLUSTER NODES'.split(' ')).decode(
                         'UTF-8').splitlines()]
        all_nodes = [{
                    'id': x[0],
                    'ip_port': x[1],
                    'is_number_1': 'myself' in x[2]
                } for x in nodes]
        # Sort randomly, but ensure that the master is always in [0]
        master = [x for x in all_nodes if x['is_number_1']]
        slaves = [x for x in all_nodes if not x['is_number_1']]
        random.shuffle(slaves)
        return master + slaves

    def _get_nodes_primitive(self):
        container_ids = subprocess.check_output('docker-compose ps -q redis-master', shell=True).decode().splitlines()
        return [(self.dckr.inspect_container(c)['NetworkSettings']['Networks']['erasuretester_default']['IPAddress'], 6379) for c in container_ids]

    def _kill_a_node(self):
        victim = self.nodes.pop()

        # Brutally kill the node
        container_name = socket.gethostbyaddr(victim['ip_port'].split(':')[0])[0].split('.')[0]
        self.dckr.kill(container_name)
        self.dckr.remove_container(container_name, force=True)

        for node in self.nodes:
            subprocess.check_call(['redis-cli', '-h', node['ip_port'].split(':')[0], 'CLUSTER', 'FORGET', victim['id']])

        redistrib = subprocess.Popen(['ruby', 'redis-trib.rb', 'fix', self.nodes[0]['ip_port']], stdin=subprocess.PIPE, stdout=subprocess.DEVNULL)
        redistrib.communicate(b'yes\n')
        redistrib.wait()
        sleep(3)

    def _remove_a_node(self):
        victim = self.nodes.pop()
        master_ip_port = [x['ip_port'] for x in self.nodes if x['is_number_1']][0]
        info = subprocess.check_output(['ruby', 'redis-trib.rb', 'info', master_ip_port]).decode('UTF-8').splitlines()
        slots_to_remove = int(
                re.search(r'([0-9]+) slots', [x for x in info if x.startswith(victim['ip_port'])][0]).group(1))
        slots_to_remove_per_node = int(slots_to_remove / self.cluster_size)
        slots_to_remove -= slots_to_remove_per_node * self.cluster_size

        for node in self.nodes:
            self._transfer_slots(victim['id'], node['id'], slots_to_remove_per_node, master_ip_port)
        if slots_to_remove > 0:
            self._transfer_slots(victim['id'], self.nodes[0]['id'], slots_to_remove, master_ip_port)

        subprocess.check_call(['ruby', 'redis-trib.rb', 'del-node', master_ip_port, victim['id']])
        self.cluster_size -= 1
        self.nodes = self._get_running_nodes()

    def _start_cluster(self):
        args = ['ruby', 'redis-trib.rb', 'create']
        args += [':'.join(map(str, x)) for x in self._get_nodes_primitive()]

        redistrib = subprocess.Popen(args, stdin=subprocess.PIPE)
        redistrib.communicate(b'yes\n')
        redistrib.wait()

        self.nodes = self._get_running_nodes()
