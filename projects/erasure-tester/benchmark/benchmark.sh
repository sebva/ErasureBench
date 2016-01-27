#!/usr/bin/env sh

rpcbind
rpc.statd
sleep 10
echo Mounting NFS filesystem...
mount -a
sleep 2

echo Starting Python client...
exec python3 -u /opt/erasuretester/benchmark-client.py
