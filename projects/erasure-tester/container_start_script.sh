#!/usr/bin/env bash
# This script needs to run in the container

rpcbind
service nfs-kernel-server start

echo Filesystem access using NFS at `ip -4 addr | grep inet | tail -n 1 | sed -r 's/.+inet ([0-9.]+).*/\1/g'`:/mnt/erasure

sleep 20

master_ip=`grep erasuretester_redis-master_1 /etc/hosts | grep -Eo '[0-9.]{7,}' | head -n 1`
echo Master is at ${master_ip}

echo Assigning slots to the different nodes...
echo yes | ruby ./redis-trib.rb fix ${master_ip}:6379 > /dev/null

sleep 3 # Let the cluster transition to state 'ok'

echo Starting Java program...
REDIS_ADDRESS=${master_ip}:6379 exec java -cp '*' ch.unine.vauchers.erasuretester.Main /mnt/erasure
