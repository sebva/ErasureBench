#!/usr/bin/env python3
# Script that creates the .dat file for trace-plot.tex
# Reads the output of `tshark -n -l -q -T fields -e frame.time_relative -e frame.len -e redis.value` as input

import sys


def hour(sec):
    return float(sec) / 3600


def minute(sec):
    return float(sec) / 60

timeres = 20

general_count = 0
write_count = 0
read_count = 0
redis_count = 0
cluster_count = 0
exists_count = 0

current_time = 0
last_print = 0

timestamp = 0.0
length = 0
time_bracket = 0

print("sec\tminute\thour\tgeneral\twrite\tread\tredis\tcluster\texists")

for line in sys.stdin:
    try:
        cols = line.split('\t')
        if len(cols) < 3:
            continue
        timestamp = float(cols[0])
        length = int(cols[1])
        redis = cols[2]
        if redis.strip() != '':
            if redis.startswith('SET'):
                write_count += length
            elif redis.startswith('AA') or redis.startswith('GET'):
                read_count += length
            elif redis.startswith('cluster'):
                cluster_count += length
            elif redis.startswith('EXISTS') or redis == '1' or redis == '0':
                exists_count += length
            else:
                redis_count += length
        else:
            general_count += length
        time_bracket = int(timestamp // timeres) * timeres
        if time_bracket > current_time:
            for i in range(last_print + timeres, time_bracket - timeres, timeres):
                print("{}\t{}\t{}\t0\t0\t0\t0\t0\t0".format(i, minute(i), hour(i)))
                last_print = i
                current_time = i + timeres
        
            print("{}\t{}\t{}\t{}\t{}\t{}\t{}\t{}\t{}".format(current_time, minute(current_time), hour(current_time), general_count, write_count, read_count, redis_count, cluster_count, exists_count))
            last_print = current_time
            current_time = time_bracket
            general_count = 0
            write_count = 0
            read_count = 0
            redis_count = 0
            cluster_count = 0
            exists_count = 0
    except:
        pass
