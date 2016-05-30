FROM debian:jessie

RUN apt-get -yqq update && \
    apt-get -yqq install --no-install-recommends python3-pip && \
    apt-get -yqq clean

RUN pip3 install docker-compose pydevd

COPY *.sh *.py *.rb docker-compose.yml /opt/erasuretester/
RUN chmod +x /opt/erasuretester/benchmark.py && \
    chmod +x /opt/erasuretester/container_start_script.sh

WORKDIR /opt/erasuretester

EXPOSE 111/udp 2049/tcp

CMD ["/opt/erasuretester/container_start_script.sh"]
