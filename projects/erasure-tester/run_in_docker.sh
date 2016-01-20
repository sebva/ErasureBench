#!/bin/sh

./gradlew docker
docker run --cap-add SYS_ADMIN --device /dev/fuse -it erasuretester:latest
