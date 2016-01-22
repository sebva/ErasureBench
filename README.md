# Master Thesis

## Report

The report is written in LaTeX, and can be compiled using a standard distribution like TeXlive. A Makefile is available, so the report can be generated using:
```
$ cd report
$ make
```

To compile the bibliography, [Biber](http://biblatex-biber.sourceforge.net/) is needed.

## Projects descriptions

### Erasure-Tester

This project is a Java application that provides an interface between FUSE and different erasure code implementations. The backend used to store individual blocks can be replaced.

The Gradle building system is used to compile and run the project. The easiest way to mount an instance of the tester is to fire up some Docker containers.

Under a Debian GNU/Linux (Jessie) system, all needed dependencies can be installed and configured through the following commands:

```
$ sudo apt-key adv --keyserver hkp://p80.pool.sks-keyservers.net:80 --recv-keys 58118E89F3A912897C070ADBF76221572C52609D
$ echo 'deb https://apt.dockerproject.org/repo debian-jessie main' | sudo tee /etc/apt/sources.list.d/docker.list
$ sudo apt-get update
$ sudo apt-get install docker-engine ruby-redis nfs-common
$ sudo pip install docker-compose
$ sudo usermod -a -G docker <YOUR-USERNAME>
$ sudo systemctl start docker
```

You then have to log-off and then back on to apply the group modification.

In order to orchestrate multiple containers at once, we use the [Docker compose](https://docs.docker.com/compose/install/) tool that needs to be installed separately from the main Docker engine.

Then, the entire application, including a cluster of 3 Redis servers, can be started using:

```
$ cd projects/erasure-tester
$ ./run_in_docker.sh
```

_If the Java application crashes with a message about the cluster being in a failed state, the easiest way to solve the problem is to execute the following commands:_

```
docker-compose --x-networking scale erasure=0
docker-compose --x-networking scale erasure=1
docker-compose --x-networking logs erasure
```

When the containers are started, the exposed FUSE filesystem can be mounted in the host computer via NFS:

```
$ sudo mount `docker inspect erasuretester_erasure_1 | grep IPAddress | grep -Eo '[0-9.]{7,}' | head -n 1`:/mnt/erasure /mnt/<host-mountpoint>
```

**Do not forget to unmount the NFS filesystem from the host before stopping the containers!**

```
$ sudo umount /mnt/my-mountpoint
```

