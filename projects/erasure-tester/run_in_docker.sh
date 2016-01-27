#!/bin/sh

# Clean everything at Ctrl+C
# trap 'docker-compose --x-networking stop && yes y | docker-compose --x-networking rm' TERM INT

./gradlew --daemon docker \
&& docker-compose --x-networking scale redis-master=1 redis-slave=2 erasure=1 \
&& docker-compose --x-networking logs erasure
