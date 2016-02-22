#!/usr/bin/env bash
# This script needs to run in the container

rpcbind
service nfs-kernel-server start

echo Filesystem access using NFS at `ip -4 addr | grep inet | tail -n 1 | sed -r 's/.+inet ([0-9.]+).*/\1/g'`:/mnt/erasure

sleep 3

echo Starting Java program...
exec java -cp '*' ch.unine.vauchers.erasuretester.Main -q -s Memory /mnt/erasure
