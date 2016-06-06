#!/usr/bin/env bash
# This script will run the benchmarks on a remote Docker instance, which can be a Docker Swarm.
# There must be a Docker repository running on port 5000 of the remote machine.

MANAGER_IP=10.100.0.22

echo 'Password of the user on the remote machine (Press Enter if using ssh keys):'
read -s password

./gradlew --daemon docker

# Not very secure way of passing the password...
sshpass -p "${password}" ssh -N -L 5000:localhost:5000 debian@${MANAGER_IP} &
ssh_pid=$!

# Wait for the tunnel to be ready
sleep 5

docker tag erasuretester:latest localhost:5000/erasuretester:latest
docker tag redis-master:latest localhost:5000/redis-master:latest
docker push localhost:5000/erasuretester:latest
docker push localhost:5000/redis-master:latest

kill ${ssh_pid}

sshpass -p "${password}" ssh debian@${MANAGER_IP} 'docker -H tcp://0.0.0.0:5732 stop $(docker -H tcp://0.0.0.0:5732 ps -q) && docker -H tcp://0.0.0.0:5732 rm $(docker -H tcp://0.0.0.0:5732 ps -aq)'

sshpass -p "${password}" rsync -av --copy-links cluster/ debian@${MANAGER_IP}:~/erasuretester
# Uncomment to launch the benchmarks directly
# It is recommended to launch them separately in e.g. a tmux session
# sshpass -p "${password}" ssh debian@${MANAGER_IP} 'cd ~/erasuretester && exec ./run_benchmarks.sh'
