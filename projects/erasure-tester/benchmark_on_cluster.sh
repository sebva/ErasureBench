#!/usr/bin/env bash

MANAGER_IP=172.16.0.167

echo Password:
read -s password

./gradlew --daemon docker

# Not very secure way of passing the password...
sshpass -p "${password}" ssh -N -L 5000:localhost:5000 debian@${MANAGER_IP} &
ssh_pid=$!

sleep 5

docker tag erasuretester:latest localhost:5000/erasuretester:latest
docker tag redis-master:latest localhost:5000/redis-master:latest
docker push localhost:5000/erasuretester:latest
docker push localhost:5000/redis-master:latest

kill ${ssh_pid}

sshpass -p "${password}" ssh debian@${MANAGER_IP} 'docker -H tcp://0.0.0.0:5732 stop $(docker ps -q) && docker -H tcp://0.0.0.0:5732 rm $(docker ps -aq)'

sshpass -p "${password}" rsync -av --copy-links cluster/ debian@${MANAGER_IP}:~/erasuretester
sshpass -p "${password}" ssh debian@${MANAGER_IP} 'cd ~/erasuretester && exec ./run_benchmarks.sh'
