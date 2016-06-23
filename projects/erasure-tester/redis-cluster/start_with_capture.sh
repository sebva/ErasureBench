#!/usr/bin/env bash
trap 'kill $(jobs -p)' EXIT

id=$(cut -c -8 /etc/hostname)
/usr/local/bin/docker-entrypoint.sh "$@" &
redis_pid=$!
filename="/capture/rediscapture_$(date +%s)_${id}.csv"
dstat -n --output ${filename} --noupdate 5 > /dev/null &
wait ${redis_pid}
