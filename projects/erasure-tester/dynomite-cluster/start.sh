#!/usr/bin/env bash

echo "Starting Redis"
service redis-server start

# The token is the 8 first characters of the container ID (which is hexadecimal), converted to int.
hostname_hex=$(cut -c -8 /etc/hostname)
token=$((0x${hostname_hex}))
echo "Token is ${token}"
sed -i s/replace_token_here/${token}/ /opt/dynomite.yaml

export DYNOMITE_FLORIDA_IP=$(dig +short erasuretester_benchmark_1 | head -n1)
export DYNOMITE_FLORIDA_PORT=4321

echo "Florida at ${DYNOMITE_FLORIDA_IP}:${DYNOMITE_FLORIDA_PORT}"

exec dynomite -c /opt/dynomite.yaml
