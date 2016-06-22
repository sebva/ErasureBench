FROM redis:3.2

RUN apt-get -yqq update && apt-get -yqq install --no-install-recommends wireshark-common && apt-get -yqq clean

# The original Redis image uses a volume in /data. We do not want persistency, so we use another workdir.
RUN mkdir -p /workdir && chown redis:redis /workdir
WORKDIR /workdir

RUN mkdir -p /capture && chown 0:0 /capture

COPY redis.conf /usr/local/etc/redis/redis.conf
COPY start_with_capture.sh /workdir/start_with_capture.sh
RUN chmod +x /workdir/start_with_capture.sh

ENTRYPOINT ["/workdir/start_with_capture.sh"]
CMD ["redis-server", "/usr/local/etc/redis/redis.conf"]
