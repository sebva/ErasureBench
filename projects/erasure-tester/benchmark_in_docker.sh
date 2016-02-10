#!/bin/sh

./gradlew --daemon docker

mkdir -p results

# Clean everything at Ctrl+C
trap 'docker-compose down && exit' TERM INT

docker-compose up -d redis-standalone
docker-compose scale redis-master=5 redis-standalone=1 erasure=0 benchmark=1 # For 5 Redis servers
docker-compose logs benchmark
docker-compose down
