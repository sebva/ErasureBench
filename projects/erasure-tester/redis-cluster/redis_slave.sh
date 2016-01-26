#!/usr/bin/env sh

redis-server /usr/local/etc/redis/redis.conf &
PID=$!

sleep 3

master=`nslookup erasuretester_redis-master_1 | grep -oE [0-9.]{7,} | head -n 1`
echo Master is at ${master}
redis-cli CLUSTER MEET ${master} 6379

trap 'kill -SIGTERM $PID' SIGTERM SIGINT
wait $PID
trap - SIGTERM SIGINT
wait $PID
EXIT_STATUS=$?
