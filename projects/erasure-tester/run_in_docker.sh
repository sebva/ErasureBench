#!/bin/sh

./gradlew docker
docker run --cap-add SYS_ADMIN --device /dev/fuse erasuretester:latest
