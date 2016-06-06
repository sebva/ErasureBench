# Docker Swarm instructions

These instructions explain how to install and configure a Docker Swarm cluster.
We use the [Debian](https://www.debian.org) operating system on virtual machines managed by [OpenNebula](http://opennebula.org/). The manager is called swarm-m, each slave is called swarm-n with n starting at 1.

## Create the image

These steps are to be performed in a local virtual machine running in KVM.

* Install Debian stable
* Install the kernel from the backports
* Install Docker
* Install Docker-Compose
* Install Consul
* Create /etc/systemd/system/docker.service.d/docker.conf with the following content:
```
[Service]
ExecStart=
ExecStart=/usr/bin/docker -H fd:// -H tcp://0.0.0.0:2375 -H unix:///var/run/docker.sock --cluster-advertise eth0:2375 --cluster-store consul://127.0.0.1:8500
```
* When running on an environment where IPs in the 172.16.0.0/12 subnet might be in use, it is wise to tell Docker to use another RFC 1918 subnet. Example to add to ExecStart (note that it is a machine IP, not the network IP):
```
--bip=10.99.0.1/24
```
* We have identified a [bug](https://github.com/docker/swarm/issues/2181) that can lead to the communication between containers being broken. Our fix is to disable the userland proxy.
```
--userland-proxy=false
```
* Install and configure zsh with zsh-antigen
    * Use the following .zshrc:
```zsh
source /usr/share/zsh-antigen/antigen.zsh

antigen use oh-my-zsh

antigen bundle git
antigen bundle debian
antigen bundle docker
antigen bundle sudo
antigen bundle command-not-found

antigen bundle zsh-users/zsh-history-substring-search
antigen bundle zsh-users/zsh-syntax-highlighting

antigen theme agnoster

antigen apply

export apt_pref=apt-get
alias ldocker="docker -H tcp://0.0.0.0:2375"
alias swarm-docker="docker -H tcp://0.0.0.0:5732"
export IP=$(ip route get 8.8.8.8 | awk 'NR==1 {print $NF}')
```
* Install opennebula-context
* Delete /etc/docker/key.json

## Configure the running system

You can now copy your image to the OpenNebula cluster. Start one machine as swarm-m, which will be the manager, and as many slaves as you want.

### All machines

* Edit /etc/fstab to mount the secondary disk at /var/lib/docker
* Set the hostname in /etc/hostname and /etc/hosts to the correct value (swarm-N for the slave, swarm-m for the manager)


### Manager

* Create the X.509 certificate for the registry
    * Declare swarm-m as CN, and empty values for the rest.
    * Copy the created domain.crt as /etc/docker/certs.d/swarm-m:5000/ca.crt on ALL machines
        * (One can use the Python HTTP server for this)
```
mkdir -p certs && openssl req \
  -newkey rsa:4096 -nodes -sha256 -keyout certs/domain.key \
  -x509 -days 365 -out certs/domain.crt
```

### Slaves

* Set the association between swarm-m and the manager's IP in /etc/hosts

## Run the swarm

### Manager

* Start consul master in a tmux
```
consul agent -server -bootstrap-expect 1 -data-dir /tmp/consul -node=master -bind=$IP -client $IP
```
* Create nodes discovery file at /tmp/cluster.disco
    * Enter one slave IP:port per line, like that:
```
192.168.196.16:2375
192.168.196.17:2375
```

### Slaves

* Start consul member in a tmux
```
consul agent -data-dir /tmp/consul -node=$HOST -bind=$IP
```
* Join consul
```
consul join -rpc-addr=MANAGER_IP:8400 $IP
```

### All nodes

* Restart Docker
```
sudo systemctl restart docker
```

### Manager

* Start Swarm
```
ldocker run -v /tmp/cluster.disco:/tmp/cluster.disco -d -p 5732:2375 swarm manage file:///tmp/cluster.disco
```
* Check Swarm with swarm-docker info
* Start registry
```
docker run -d -p 5000:5000 --restart=always --name registry \
  -v `pwd`/certs:/certs \
  -e REGISTRY_HTTP_TLS_CERTIFICATE=/certs/domain.crt \
  -e REGISTRY_HTTP_TLS_KEY=/certs/domain.key \
  registry:2
```

## Deploy an application

Automated deployment: _(You still have to collect the results manually from one slave)_
```
./benchmark_on_cluster.sh
```

Following is the manual methodology:

### Build machine

* Build the app normally
* Tag each container with:
```
docker tag CONTAINER_NAME:latest localhost:5000/CONTAINER_NAME:latest
```
* Forward port 5000 of the manager to port 5000 of localhost via SSH
* Push each image to the manager's registry
```
docker push localhost:5000/CONTAINER_NAME:latest
```
* Copy docker-compose.yml to the manager
    * The file MUST be in a folder called erasuretester

### Manager

Pull all required images in advance. Example:
```
swarm-docker pull swarm-m:5000/CONTAINER_NAME:latest
```

Also clean any stopped container so that Swarm can correctly schedule the execution on all nodes.
```
swarm-docker rm $(swarm-docker ps -aq)
```

* Wait until 'swarm-docker info' reports 0 containers
* Set the default Docker to be the Swarm manager
```
export DOCKER_HOST=tcp://0.0.0.0:5732
```
* Bring the system up
    * Use the same command as in benchmark_in_docker.sh
* Collect the results in the slave node that ran the erasuretester container
    * Use watch docker ps during the execution to see which node is the wanted one

## Run benchmarks

We assume that the automated script was used.

* In a tmux, start ~/erasuretester/run_benchmarks.sh
* When the execution is finished, collect the results from each nodes, for example using rsync:
```
for i (232 233 234 236 237 238 239 240 241 243 245 249 250 251); do
	rsync -a -v "debian@172.16.1.${i}:~/erasuretester/results/" .
done
```
