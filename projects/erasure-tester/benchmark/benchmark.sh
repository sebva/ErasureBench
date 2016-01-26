#!/usr/bin/env sh

rpcbind
rpc.statd
sleep 3
mount -a
sleep 2

echo Writing 40 kB using dd
dd if=/dev/zero of=/mnt/erasure/file bs=4096 count=10
