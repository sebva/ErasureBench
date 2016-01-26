FROM redis:alpine

COPY redis.conf /usr/local/etc/redis/redis.conf
COPY redis_slave.sh /opt/redis.sh
RUN chmod +x /opt/redis.sh
CMD ["/opt/redis.sh"]
