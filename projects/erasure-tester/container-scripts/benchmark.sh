#!/usr/bin/env sh

sleep 10

master_ip=`grep erasuretester_redis-master_1 /etc/hosts | grep -Eo '[0-9.]{7,}' | head -n 1`
echo Master is at ${master_ip}

echo Starting Python client...
REDIS_ADDRESS=${master_ip}:6379 exec python3 -u /opt/erasuretester/benchmark.py
