#!/bin/sh
# Execute this script to run the benchmarks in the local Docker instance.


# Compile everything and assemble the containers
./gradlew --daemon docker

mkdir -p results

# Clean everything at Ctrl+C
trap 'docker-compose down && exit' TERM INT

docker-compose up benchmark
