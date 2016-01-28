#!/bin/sh

./gradlew --daemon docker

mkdir -p results

# Clean everything at Ctrl+C
trap 'docker-compose --x-networking stop && yes y | docker-compose --x-networking rm && exit' TERM INT

docker-compose --x-networking scale redis-master=5 erasure=0 benchmark=1 # For 5 Redis servers
docker-compose --x-networking logs benchmark
docker-compose --x-networking stop && yes y | docker-compose --x-networking rm
