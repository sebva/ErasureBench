FROM debian:jessie

RUN echo 'deb http://mirror.switch.ch/ftp/mirror/debian/ jessie-backports main' >> /etc/apt/sources.list && \
    apt-get -yqq update && \
    apt-get -yqq dist-upgrade && \
    apt-get -yqq install --no-install-recommends openjdk-8-jre-headless libfuse2 nfs-kernel-server ruby-redis python3-pip redis-tools gnuplot5-nox wget bzip2 && \
    apt-get -yqq clean

RUN pip3 install docker-compose pydevd

RUN mkdir -p /opt/erasuretester/results

RUN echo '79c1a9d34e904413f5fb659980df60fc6d7f199e /opt/erasuretester/httpd.tar.bz2' > /opt/erasuretester/httpd-2.4.18.tar.bz2.sha1 && \
    wget -q -O /opt/erasuretester/httpd.tar.bz2 https://dl.dropboxusercontent.com/u/31349479/thesis/httpd-2.4.18.tar.bz2 && \
    sha1sum -c /opt/erasuretester/httpd-2.4.18.tar.bz2.sha1 && \
    tar -jxf /opt/erasuretester/httpd.tar.bz2 -C /tmp && \
    find /tmp/httpd-2.4.18 -type f -exec sha256sum {} \; | sed s-/tmp/-/mnt/erasure/- > /opt/erasuretester/httpd.sha256 && \
    rm -rf /tmp/httpd-2.4.18

RUN echo 'c8f258a7355b40a485007c40865480349c157292 /opt/erasuretester/bc.tar.gz' > /opt/erasuretester/bc-1.06.tar.gz.sha1 && \
    wget -q -O /opt/erasuretester/bc.tar.gz http://mirror.switch.ch/ftp/mirror/gnu/bc/bc-1.06.tar.gz && \
    sha1sum -c /opt/erasuretester/bc-1.06.tar.gz.sha1 && \
    tar -zxf /opt/erasuretester/bc.tar.gz -C /tmp && \
    find /tmp/bc-1.06 -type f -exec sha256sum {} \; | sed s-/tmp/-/mnt/erasure/- > /opt/erasuretester/bc.sha256 && \
    rm -rf /tmp/bc-1.06

RUN echo 'f34d73521f34681ac651f1ddd85a837d70782c45 /opt/erasuretester/10bytes.tar.bz2' > /opt/erasuretester/10bytes.tar.bz2.sha1 && \
    wget -q -O /opt/erasuretester/10bytes.tar.bz2 https://dl.dropboxusercontent.com/u/31349479/thesis/10bytes.tar.bz2 && \
    sha1sum -c /opt/erasuretester/10bytes.tar.bz2.sha1 && \
    tar -jxf /opt/erasuretester/10bytes.tar.bz2 -C /tmp && \
    find /tmp/10bytes -type f -exec sha256sum {} \; | sed s-/tmp/-/mnt/erasure/- > /opt/erasuretester/10bytes.sha256 && \
    rm -rf /tmp/10bytes

RUN mkdir /mnt/erasure && \
    echo "/mnt/erasure\t*(rw,fsid=0,no_subtree_check,no_root_squash)" > /etc/exports

COPY *-all.jar /opt/erasuretester/
COPY *.sh *.py *.rb docker-compose.yml /opt/erasuretester/
RUN chmod +x /opt/erasuretester/benchmark.py && \
    chmod +x /opt/erasuretester/container_start_script.sh

WORKDIR /opt/erasuretester

EXPOSE 111/udp 2049/tcp

CMD ["/opt/erasuretester/container_start_script.sh"]
