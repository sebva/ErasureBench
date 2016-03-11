import socket
import subprocess

import re
from time import sleep


class RedisCluster:
    nodes = []

    def __init__(self, cluster_size=3):
        self.cluster_size = cluster_size

    def __enter__(self):
        print("Starting a Redis cluster of %d nodes" % self.cluster_size)
        if self.cluster_size == 0:
            print("Nothing to do")
            return self
        elif self.cluster_size == 1:
            self._docker_scale(1, standalone=True)
            sleep(5)
            return self

        self._docker_scale(self.cluster_size)
        primitive_nodes = []
        while len(primitive_nodes) < self.cluster_size:
            print("Waiting for all nodes to come alive...")
            sleep(5)
            primitive_nodes = self._get_nodes_primitive()

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
            self._add_new_nodes(new_cluster_size, self.nodes)
        else:
            while len(self.nodes) > new_cluster_size:
                if brutal:
                    self._kill_a_node()
                else:
                    self._remove_a_node()

        self.cluster_size = new_cluster_size
        self.nodes = self._get_running_nodes()
        return len(self.nodes) == self.cluster_size

    @staticmethod
    def get_master_node_str(redis_size):
        container_name = 'standalone' if redis_size <= 1 else 'master'
        return ':'.join(
                map(str, socket.getaddrinfo('erasuretester_redis-%s_1' % container_name, 6379, socket.AF_INET)[0][4]))

    def flushall(self):
        if self.cluster_size == 1:
            subprocess.check_call(['redis-cli', '-h', 'erasuretester_redis-standalone_1', 'FLUSHALL'])
        elif self.cluster_size >= 2:
            nodes = self._get_running_nodes()
            for node in nodes:
                subprocess.check_call(['redis-cli', '-h', node['ip_port'].split(':')[0], 'FLUSHALL'])

    def _add_new_nodes(self, cluster_size, old_nodes):
        self._docker_scale(cluster_size)
        nb_new_nodes = cluster_size - len(old_nodes)

        new_ips = [':'.join(map(str, x)) for x in self.nodes[-nb_new_nodes:]]
        print(new_ips)
        master_ip_port = old_nodes[0]['ip_port']

        for ip in new_ips:
            sleep(1)
            subprocess.check_call(['ruby', 'redis-trib.rb', 'add-node', ip, master_ip_port])

        sleep(2)
        new_nodes = [x for x in self._get_running_nodes() if x['ip_port'] in new_ips]
        shards_to_move_per_node = round(16384 / cluster_size / len(old_nodes))

        for new_node in new_nodes:
            for old_node in old_nodes:
                sleep(0.5)
                fix = subprocess.Popen(['ruby', 'redis-trib.rb', 'fix', master_ip_port], stdin=subprocess.PIPE, stdout=subprocess.DEVNULL)
                fix.communicate(b'yes\n')
                fix.wait()
                self._transfer_slots(old_node['id'], new_node['id'], shards_to_move_per_node, master_ip_port)

        subprocess.check_call(['ruby', 'redis-trib.rb', 'info', master_ip_port])

    @staticmethod
    def _docker_scale(cluster_size, standalone=False):
        node_type = "standalone" if standalone else "master"
        subprocess.check_call(['docker-compose', 'scale', 'redis-%s=%d' % (node_type, cluster_size)])

    @staticmethod
    def _get_running_nodes():
        nodes = [x.split(' ') for x in
                 subprocess.check_output('redis-cli -h erasuretester_redis-master_1 CLUSTER NODES'.split(' ')).decode(
                         'UTF-8').splitlines()]
        return [{
                    'id': x[0],
                    'ip_port': x[1],
                    'is_number_1': 'myself' in x[2]
                } for x in nodes]

    @staticmethod
    def _get_nodes_primitive():
        redis_nodes = []
        try:
            i = 1
            while True:
                redis_nodes.append(socket.getaddrinfo('erasuretester_redis-master_%d' % i, 6379, socket.AF_INET)[0][4])
                i += 1
        except socket.gaierror:
            pass
        return redis_nodes

    def _elect_victim(self):
        return self.nodes[-1]

    def _kill_a_node(self):
        victim = self._elect_victim()

        # Brutally kill the node
        subprocess.check_call(['redis-cli', '-h', victim['ip_port'].split(':')[0], 'SHUTDOWN'])
        for node in self.nodes:
            subprocess.check_call(['redis-cli', '-h', node['ip_port'].split(':')[0], 'CLUSTER', 'FORGET', victim['id']])

        redistrib = subprocess.Popen(['ruby', 'redis-trib.rb', 'fix', self.nodes[0]['ip_port']], stdin=subprocess.PIPE, stdout=subprocess.DEVNULL)
        redistrib.communicate(b'yes\n')
        redistrib.wait()

    def _remove_a_node(self):
        victim = self._elect_victim()
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

    @staticmethod
    def _transfer_slots(from_id, to_id, amount, master_ip_port):
        print('Transfering %d slots...' % amount)
        subprocess.check_call(('ruby redis-trib.rb reshard --from %s --to %s --slots %d --yes %s' % (
            from_id, to_id, amount, master_ip_port)).split(' '), stdout=subprocess.DEVNULL)

    def _start_cluster(self):
        args = ['ruby', 'redis-trib.rb', 'create']
        args += [':'.join(map(str, x)) for x in self._get_nodes_primitive()]

        redistrib = subprocess.Popen(args, stdin=subprocess.PIPE)
        redistrib.communicate(b'yes\n')
        redistrib.wait()

        self.nodes = self._get_running_nodes()
