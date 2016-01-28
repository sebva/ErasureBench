FROM debian:jessie

RUN echo 'deb http://mirror.switch.ch/ftp/mirror/debian/ jessie-backports main' >> /etc/apt/sources.list
RUN apt-get -yqq update
RUN apt-get -yqq dist-upgrade
RUN apt-get -yqq install --no-install-recommends openjdk-8-jre-headless libfuse2 nfs-kernel-server ruby-redis python3-pip
RUN pip3 install docker-py

RUN mkdir /mnt/erasure
RUN echo "/mnt/erasure\t*(rw,fsid=0,no_subtree_check,no_root_squash)" > /etc/exports

RUN mkdir -p /opt/erasuretester/results
COPY *-all.jar /opt/erasuretester/
COPY *.sh /opt/erasuretester/
COPY *.py /opt/erasuretester/
COPY *.rb /opt/erasuretester/
RUN chmod +x /opt/erasuretester/benchmark.sh
RUN chmod +x /opt/erasuretester/container_start_script.sh
WORKDIR /opt/erasuretester

EXPOSE 111/udp 2049/tcp

CMD ["/opt/erasuretester/container_start_script.sh"]
