FROM dynomitedb/dynomite

RUN apt-get -yqq update && apt-get -yqq install --no-install-recommends dnsutils redis-server && apt-get clean

COPY dynomite.pem /opt/dynomite.pem
COPY start.sh /opt/start.sh
RUN chmod +x /opt/start.sh
COPY dynomite.yml /opt/dynomite.yaml

CMD ["/opt/start.sh"]
