#!/usr/bin/env python3

import os
import sqlite3


def create_db(sql):
    c = sql.cursor()
    c.executescript(r'''
    CREATE TABLE component
(
    component_id INTEGER DEFAULT '0' NOT NULL,
    node_id INTEGER DEFAULT '0' NOT NULL,
    platform_id INTEGER DEFAULT '0' NOT NULL,
    creator_id INTEGER DEFAULT NULL,
    node_name TEXT DEFAULT 'NULL',
    component_type INTEGER DEFAULT NULL,
    trace_start REAL DEFAULT NULL,
    trace_end REAL DEFAULT NULL,
    resolution REAL DEFAULT NULL,
    PRIMARY KEY ("component_id", "node_id", "platform_id")
);
CREATE TABLE creator
(
    creator_id INTEGER DEFAULT '0' NOT NULL,
    component_id INTEGER DEFAULT '0' NOT NULL,
    node_id INTEGER DEFAULT '0' NOT NULL,
    platform_id INTEGER DEFAULT '0' NOT NULL,
    creator TEXT DEFAULT 'NULL',
    cite TEXT DEFAULT 'NULL',
    copyright TEXT DEFAULT 'NULL',
    PRIMARY KEY ("creator_id", "component_id", "node_id", "platform_id")
);
CREATE TABLE event_state
(
    event_id INTEGER DEFAULT '0' NOT NULL,
    component_id INTEGER DEFAULT '0' NOT NULL,
    node_id INTEGER DEFAULT '0' NOT NULL,
    platform_id INTEGER DEFAULT '0' NOT NULL,
    i_val INTEGER DEFAULT NULL,
    f_val REAL DEFAULT NULL,
    s_val TEXT DEFAULT 'NULL',
    PRIMARY KEY ("event_id", "component_id", "node_id", "platform_id")
);
CREATE TABLE event_trace
(
    event_id INTEGER DEFAULT '0' NOT NULL,
    component_id INTEGER DEFAULT '0' NOT NULL,
    node_id INTEGER DEFAULT '0' NOT NULL,
    platform_id INTEGER DEFAULT '0' NOT NULL,
    node_name TEXT DEFAULT 'NULL',
    event_type INTEGER DEFAULT NULL,
    event_start_time REAL DEFAULT NULL,
    event_end_time REAL DEFAULT NULL,
    event_end_reason INTEGER DEFAULT NULL,
    PRIMARY KEY ("event_id", "component_id", "node_id", "platform_id")
);
CREATE TABLE node
(
    node_id INTEGER DEFAULT '0' NOT NULL,
    platform_id INTEGER DEFAULT '0' NOT NULL,
    node_name TEXT DEFAULT 'NULL',
    node_ip TEXT DEFAULT 'NULL',
    node_location TEXT DEFAULT 'NULL',
    timezone INTEGER DEFAULT NULL,
    proc_model TEXT DEFAULT 'NULL',
    os_name TEXT DEFAULT 'NULL',
    cores_per_proc INTEGER DEFAULT NULL,
    num_procs INTEGER DEFAULT NULL,
    mem_size REAL DEFAULT NULL,
    disk_size REAL DEFAULT NULL,
    up_bw REAL DEFAULT NULL,
    down_bw REAL DEFAULT NULL,
    metric_id REAL DEFAULT NULL,
    notes TEXT DEFAULT 'NULL',
    PRIMARY KEY ("node_id", "platform_id")
);
CREATE TABLE node_perf
(
    metric_id INTEGER DEFAULT '0' NOT NULL,
    node_id INTEGER DEFAULT '0' NOT NULL,
    platform_id INTEGER DEFAULT '0' NOT NULL,
    sfpop_speed REAL DEFAULT NULL,
    dfpop_speed REAL DEFAULT NULL,
    iop_speed REAL DEFAULT NULL,
    i_val INTEGER DEFAULT NULL,
    f_val REAL DEFAULT NULL,
    s_val TEXT DEFAULT 'NULL',
    PRIMARY KEY ("metric_id", "node_id", "platform_id")
);
CREATE TABLE platform
(
    platform_id INTEGER PRIMARY KEY NOT NULL,
    platform_name TEXT DEFAULT 'NULL',
    platform_location TEXT DEFAULT 'NULL',
    platform_type TEXT DEFAULT 'NULL',
    notes TEXT DEFAULT 'NULL'
);
    ''')
    sql.commit()


pk = 0


def insert(cur, node_id, up_down, time_1, time_2=0):
    global pk
    cur.execute('INSERT INTO event_trace VALUES (?, ?, ?, 1, ?, ?, ?, ?, 0)',
                (pk, node_id, node_id, "Node %d" % node_id, up_down, time_1, time_2))
    pk += 1


sql = sqlite3.connect(os.path.dirname(os.path.realpath(__file__)) + "/dummy.db")
create_db(sql)
cur = sql.cursor()
time = 0

for i in range(1, 7):
    node_id = i
    up_down = True
    insert(cur, node_id, up_down, 0, 0)

time += 30

for i in range(6, 3, -1):
    node_id = i
    up_down = False
    insert(cur, node_id, up_down, time)
    time += 30

for iteration in range(10):
    for i in zip(list(range(-2, 1)) + list(range(0, -3, -1)), ([True] * 3 + [False] * 3)):
        node_id = 6 + i[0]
        up_down = i[1]
        insert(cur, node_id, up_down, time)
        time += 30
sql.commit()
sql.close()
