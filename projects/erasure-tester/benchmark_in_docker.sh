#!/bin/sh

./gradlew --daemon docker

mkdir -p results

# Clean everything at Ctrl+C
trap 'docker-compose down && exit' TERM INT

docker-compose up -d benchmark # Redis nodes are started from the script
docker-compose logs -f -t benchmark
docker-compose down
