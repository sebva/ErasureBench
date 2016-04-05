FROM debian:jessie

RUN echo 'deb http://mirror.switch.ch/ftp/mirror/debian/ jessie-backports main' >> /etc/apt/sources.list && \
    apt-get -yqq update && \
    apt-get -yqq dist-upgrade && \
    apt-get -yqq install --no-install-recommends openjdk-8-jre-headless libfuse2 nfs-kernel-server ruby-redis python3-pip redis-tools gnuplot5-nox wget bzip2 && \
    apt-get -yqq clean

RUN pip3 install docker-compose pydevd

RUN mkdir -p /opt/erasuretester/results

RUN echo '271a129f2f04e3aa694e5c2091df9b707bf8ef80 /opt/erasuretester/httpd.tar.bz2' > /opt/erasuretester/httpd-2.4.18.tar.bz2.sha1 && \
    wget -q -O /opt/erasuretester/httpd.tar.bz2 http://mirror.switch.ch/mirror/apache/dist/httpd/httpd-2.4.18.tar.bz2 && \
    sha1sum -c /opt/erasuretester/httpd-2.4.18.tar.bz2.sha1 && \
    tar -jxf /opt/erasuretester/httpd.tar.bz2 -C /tmp && \
    find /tmp/httpd-2.4.18 -type f -exec sha256sum {} \; | sed s-/tmp/-/mnt/erasure/- > /opt/erasuretester/httpd.sha256 && \
    rm -rf /tmp/httpd-2.4.18

RUN mkdir /mnt/erasure && \
    echo "/mnt/erasure\t*(rw,fsid=0,no_subtree_check,no_root_squash)" > /etc/exports

COPY *-all.jar *.sh *.py *.rb docker-compose.yml /opt/erasuretester/
RUN chmod +x /opt/erasuretester/benchmark.py && \
    chmod +x /opt/erasuretester/container_start_script.sh

WORKDIR /opt/erasuretester

EXPOSE 111/udp 2049/tcp

CMD ["/opt/erasuretester/container_start_script.sh"]
