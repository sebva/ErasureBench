FROM redis:3.2-alpine

# The original Redis image uses a volume in /data. We do not want persistency, so we use another workdir.
RUN mkdir -p /somedirectory && chown redis:redis /somedirectory
WORKDIR /somedirectory

COPY redis.conf /usr/local/etc/redis/redis.conf
CMD ["redis-server", "/usr/local/etc/redis/redis.conf"]
