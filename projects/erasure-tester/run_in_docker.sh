#!/bin/sh

./gradlew --daemon docker
docker-compose --x-networking up -d
docker-compose --x-networking scale redis-slave=2 # For 3 total servers
docker-compose --x-networking logs erasure
