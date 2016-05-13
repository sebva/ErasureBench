#!/bin/sh

docker -H tcp://0.0.0.0:5732 pull swarm-m:5000/erasuretester:latest
docker -H tcp://0.0.0.0:5732 pull swarm-m:5000/redis-master:latest
docker -H tcp://0.0.0.0:5732 tag swarm-m:5000/erasuretester:latest erasuretester:latest
docker -H tcp://0.0.0.0:5732 tag swarm-m:5000/redis-master:latest redis-master:latest

IP=$(ip route get 8.8.8.8 | awk 'NR==1 {print $NF}')
export DOCKER_HOST=tcp://${IP}:5732

# Clean everything at Ctrl+C
trap 'docker-compose down && exit' TERM INT

docker-compose up benchmark
docker-compose down
