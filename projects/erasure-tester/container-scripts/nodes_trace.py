#!/usr/bin/env python3

import sqlite3
import time

import itertools


class NodesTrace:
    synthetic_sizes = None
    sql = None
    last_time = -1000
    begin_time = None
    current_size = 0

    def __init__(self, time_factor=1, database=None, synthetic=None, min_time=None, max_time=None):
        assert database is not None or synthetic is not None

        self.time_factor = time_factor

        if database is not None:
            self.sql = sqlite3.connect(database)
            self.cur = self.sql.cursor()
            # HUGE performance boost
            self.cur.execute(r'CREATE INDEX IF NOT EXISTS start_time_index ON event_trace (event_start_time)')
            self.cur.fetchone()

            self.cur.execute(r'SELECT MIN(event_start_time), MAX(event_start_time) FROM event_trace')
            self.min_time, self.max_time = self.cur.fetchone()
            if min_time is not None:
                self.min_time = min_time
            if max_time is not None:
                self.max_time = max_time

            self.cur.execute('''
              SELECT DISTINCT node_id FROM event_trace
                WHERE node_id IN (SELECT DISTINCT node_id FROM event_trace WHERE event_start_time <= ?)
                ORDER BY node_id
                ''', (self.min_time,))
            self.nodes_id = [x[0] for x in self.cur.fetchall()]
            self.reverse_nodes_id = dict(zip(self.nodes_id, range(len(self.nodes_id))))
        elif synthetic is not None:
            self.synthetic_sizes = synthetic
            self.synthetic_index = -1

    def next(self):
        if self.synthetic_sizes is not None:
            self.synthetic_index += 1
            if self.synthetic_index >= len(self.synthetic_sizes):
                raise StopIteration()
            else:
                next_size = self.synthetic_sizes[self.synthetic_index]

                ret = next_size, list(range(self.current_size, next_size)), list(range(next_size, self.current_size))
                self.current_size = next_size
                return ret
        else:
            if self.last_time > self.max_time:
                raise StopIteration

            now = time.time()
            if self.begin_time is None:
                self.begin_time = now
            now = (now - self.begin_time) * self.time_factor + self.begin_time

            now_db = now - self.begin_time + self.min_time

            self.cur.execute('''
              SELECT node_id, event_type FROM event_trace
                WHERE event_start_time > ?
                  AND event_start_time <= ?
                  AND node_id IN (SELECT DISTINCT node_id FROM event_trace WHERE event_start_time <= ?)
                ORDER BY node_id, event_start_time''',
                             (self.last_time, now_db, self.min_time))
            events = self.cur.fetchall()
            servers_to_kill = []
            servers_to_create = []

            nodes = {x[0] for x in events}
            for node in nodes:
                up_events = len([x for x in events if x[0] == node and x[1] == 1])
                down_events = len([x for x in events if x[0] == node and x[1] == 0])
                node_position = self.reverse_nodes_id[node]
                if up_events > down_events:
                    self.current_size += 1
                    servers_to_create.append(node_position)
                elif down_events > up_events:
                    self.current_size -= 1
                    servers_to_kill.append(node_position)

            self.last_time = now_db
            return self.current_size, servers_to_kill, servers_to_create

    def __iter__(self):
        return self

    def __next__(self):
        return self.next()

    def initial_size(self):
        if self.synthetic_sizes is not None:
            return self.synthetic_sizes[0]
        else:
            self.cur.execute('''
              SELECT COUNT(DISTINCT node_id) FROM event_trace
                WHERE event_start_time <= ?''',
                             (self.min_time,))
            return int(self.cur.fetchone()[0])


if __name__ == '__main__':
    # Test
    sut = NodesTrace(synthetic=[3, 4, 2, 3])
    count = 0
    initial = sut.initial_size()
    for r in sut:
        if count == 0:
            assert r[0] == initial
        print(r)
        assert count + len(r[1]) - len(r[2]) == r[0]
        count = r[0]

    sut = NodesTrace(time_factor=2000000, database='../../fta-parser/databases/websites02.db', min_time=100000)
    count = 0
    initial = sut.initial_size()
    print(initial)
    for r in sut:
        if count == 0:
            pass
            #assert r[0] == initial
        print(r)
        assert count + len(r[2]) - len(r[1]) == r[0]
        time.sleep(1)
        count = r[0]
