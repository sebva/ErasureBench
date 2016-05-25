#!/usr/bin/env bash
# This script needs to run in the container

sed -i s/riak@127.0.0.1/riak@${HOSTNAME}/ /etc/riak/riak.conf

riak start
tail -f /var/log/riak/console.log
