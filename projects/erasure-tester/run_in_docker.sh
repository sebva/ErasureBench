#!/bin/sh
# Run the erasure tester standalone in a Docker container.

# Clean everything at Ctrl+C
trap 'docker-compose down' TERM INT

./gradlew --daemon docker \
&& docker-compose up -d erasure \
&& docker-compose logs erasure
