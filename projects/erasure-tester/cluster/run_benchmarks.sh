#!/bin/sh

docker -H tcp://0.0.0.0:5732 pull swarm-m:5000/erasuretester:latest
docker -H tcp://0.0.0.0:5732 pull swarm-m:5000/redis-master:latest
docker -H tcp://0.0.0.0:5732 tag swarm-m:5000/erasuretester:latest erasuretester:latest
docker -H tcp://0.0.0.0:5732 tag swarm-m:5000/redis-master:latest redis-master:latest

IP=$(ip route get 8.8.8.8 | awk 'NR==1 {print $NF}')
export DOCKER_HOST=tcp://${IP}:5732

# Clean everything at Ctrl+C
trap 'docker-compose down && exit' TERM INT

docker-compose up -d benchmark
docker-compose scale redis-master=0 redis-standalone=0 erasure=0 benchmark=1
docker-compose logs benchmark
docker-compose down
