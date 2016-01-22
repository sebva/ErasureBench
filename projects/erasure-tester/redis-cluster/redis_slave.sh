#!/usr/bin/env sh
redis-server /usr/local/etc/redis/redis.conf &
sleep 10

master=`nslookup redis-master | grep -oE [0-9.]{7,} | head -n 1`
echo Master is at ${master}
redis-cli CLUSTER MEET ${master} 6379

while :
do
        sleep 30
done
