FROM debian:jessie

RUN apt-get -yqq update
RUN apt-get -yqq dist-upgrade
RUN apt-get -yqq install --no-install-recommends nfs-common python3-pip
RUN pip3 install Pyro4 docker-py

RUN mkdir /mnt/erasure
RUN echo "erasuretester_erasure_1:/mnt/erasure  /mnt/erasure  nfs  noatime,async  0  0" >> /etc/fstab

COPY benchmark.sh /opt/erasuretester/
COPY benchmark-client.py /opt/erasuretester/
RUN chmod +x /opt/erasuretester/benchmark.sh
WORKDIR /opt/erasuretester

CMD ["/opt/erasuretester/benchmark.sh"]
