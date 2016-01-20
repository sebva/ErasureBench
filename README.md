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

In order to do that, we use the [Docker compose](https://docs.docker.com/compose/install/) tool that needs to be installed separately from the main Docker engine. Then, the entire application can be started using:

```
$ cd projects/erasure-tester
$ ./gradlew docker
$ docker-compose up
```

_For lazy people, the same commands as above are contained in the_ run_in_docker.sh _script._

When the containers are started, the exposed FUSE filesystem can be mounted in the host computer via NFS. The IP and path where to connect are printed on container startup.

```
$ sudo mount <IP+Path> /mnt/my-mountpoint
```

**Do not forget to unmount the NFS filesystem from the host before stopping the containers!**

```
$ sudo umount /mnt/my-mountpoint
```