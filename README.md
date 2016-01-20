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

The Gradle building system is used to compile and run the project. The easiest way to mount an instance of the tester is to execute the following:

```
$ cd projects/erasure-tester
$ ./gradlew run -Dargs="/mnt/my-mountpoint"
```

It is possible to run the program within a Docker container. To achieve the same result as with the local startup command shown above, the following can be used:

```
$ ./gradlew docker
$ docker run --cap-add SYS_ADMIN --device /dev/fuse erasuretester:latest
$ sudo mount <container-ip>:/mnt/erasure /mnt/my-mountpoint
```

The container IP is printed to the console when the container starts.
