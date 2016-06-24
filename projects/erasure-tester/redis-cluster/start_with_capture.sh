#!/usr/bin/env bash
trap 'kill $(jobs -p)' EXIT

id=$(cut -c -8 /etc/hostname)
/usr/local/bin/docker-entrypoint.sh "$@" &
redis_pid=$!
filename="/capture/rediscapture_$(date +%s)_${id}.pcapng"
gosu 0:0 dumpcap -q -f "tcp port 6379" -i any -s 64 -w ${filename} &
wait ${redis_pid}
