FROM redis:3.2

RUN apt-get -yqq update && apt-get -yqq install --no-install-recommends wireshark-common && apt-get -yqq clean

RUN mkdir -p /capture && chown 0:0 /capture

COPY redis.conf /usr/local/etc/redis/redis.conf
COPY start_with_capture.sh /opt/start_with_capture.sh
RUN chmod +x /opt/start_with_capture.sh

ENTRYPOINT ["/opt/start_with_capture.sh"]
CMD ["redis-server", "/usr/local/etc/redis/redis.conf"]
