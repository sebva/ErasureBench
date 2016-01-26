#!/bin/sh

./gradlew --daemon docker

# Clean everything at Ctrl+C
trap 'docker-compose --x-networking stop && yes y | docker-compose --x-networking rm && exit' TERM INT

docker-compose --x-networking scale redis-master=1 redis-slave=2 erasure=1 # For 3 Redis servers
docker-compose --x-networking logs erasure &
sleep 90 # TODO
docker-compose --x-networking scale benchmark=1
docker-compose --x-networking logs benchmark
docker-compose --x-networking stop && yes y | docker-compose --x-networking rm
