FROM ubuntu:trusty

WORKDIR /opt/riak

RUN apt-get -yqq update && apt-get -yqq dist-upgrade && apt-get -yqq install wget

RUN wget -q -O riak.deb http://s3.amazonaws.com/downloads.basho.com/riak/2.1/2.1.4/ubuntu/trusty/riak_2.1.4-1_amd64.deb && \
    echo 'ae9ba5963ace86040600514d944b762f06094687 /opt/riak/riak.deb' > riak.sha1 && \
    sha1sum -c riak.sha1 && \
    dpkg -i riak.deb && \
    rm riak.deb

COPY start_script.sh start_script.sh
RUN chmod +x start_script.sh
COPY riak.conf /etc/riak/riak.conf

CMD ["/opt/riak/start_script.sh"]
