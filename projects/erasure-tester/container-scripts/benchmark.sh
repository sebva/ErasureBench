#!/usr/bin/env sh

sleep 10

master_ip=`grep erasuretester_redis-master_1 /etc/hosts | grep -Eo '[0-9.]{7,}' | head -n 1`
echo Master is at ${master_ip}

echo Assigning slots to the different nodes...
echo yes | ruby ./redis-trib.rb fix ${master_ip}:6379 > /dev/null

rpcbind
rpc.statd

echo Starting Python client...
REDIS_ADDRESS=${master_ip}:6379 exec python3 -u /opt/erasuretester/benchmark.py
