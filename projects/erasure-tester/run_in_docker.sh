#!/bin/sh

./gradlew --daemon docker
docker-compose --x-networking up -d
docker-compose --x-networking scale redis-slave=2 # For 3 total servers

sleep 6
master_ip=`docker inspect erasuretester_redis-master_1 | grep IPAddress | grep -Eo '[0-9.]{7,}' | head -n 1`
echo Assigning slots to the different nodes...
echo yes | ruby redis-cluster/redis-trib.rb fix ${master_ip}:6379 > /dev/null

docker-compose --x-networking logs erasure
