#!/usr/bin/env bash
# Alternative entrypoint that will start a Wireshark capture in parallel to Redis

trap 'kill $(jobs -p)' EXIT

id=$(cut -c -8 /etc/hostname)
/usr/local/bin/docker-entrypoint.sh "$@" &
redis_pid=$!
filename="/capture/rediscapture_$(date +%s)_${id}.pcapng"
gosu 0:0 dumpcap -q -f "tcp port 6379" -i any -w ${filename} &
wait ${redis_pid}
