#!/bin/sh

./gradlew --daemon docker

# Clean everything at Ctrl+C
trap 'docker-compose --x-networking stop && yes y | docker-compose --x-networking rm && exit' TERM INT

docker-compose --x-networking scale redis-master=1 redis-slave=4 erasure=1 benchmark=1 # For 5 Redis servers
docker-compose --x-networking logs erasure benchmark
docker-compose --x-networking stop && yes y | docker-compose --x-networking rm
