#!/bin/sh

./gradlew --daemon docker

mkdir -p results

# Clean everything at Ctrl+C
trap 'docker-compose down && exit' TERM INT

docker-compose up -d redis-standalone
docker-compose scale redis-master=0 redis-standalone=0 erasure=0 benchmark=1 # Redis nodes are started from the script
docker-compose logs benchmark
docker-compose down