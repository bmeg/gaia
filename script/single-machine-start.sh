#!/bin/bash

KAFKA_VERSION=0.10.0.1
CASSANDRA_VERSION=3.7
APACHE_MIRROR=www.gtlib.gatech.edu

BDIR="$(cd `dirname $0` && cd ../ && pwd)"
cd $BDIR

if [ ! -e kafka_2.11-$KAFKA_VERSION.tgz ]; then 
  curl -O http://$APACHE_MIRROR/pub/apache/kafka/$KAFKA_VERSION/kafka_2.11-$KAFKA_VERSION.tgz
fi

if [ ! -e kafka_2.11-$KAFKA_VERSION ]; then 
  tar xvzf kafka_2.11-$KAFKA_VERSION.tgz
fi

if [ ! -e apache-cassandra-$CASSANDRA_VERSION-bin.tar.gz ]; then
  curl -O http://$APACHE_MIRROR/pub/apache/cassandra/$CASSANDRA_VERSION/apache-cassandra-$CASSANDRA_VERSION-bin.tar.gz
fi

if [ ! -e apache-cassandra-$CASSANDRA_VERSION ]; then
  tar xvzf apache-cassandra-$CASSANDRA_VERSION-bin.tar.gz
fi

./kafka_2.11-$KAFKA_VERSION/bin/zookeeper-server-start.sh -daemon kafka_2.11-$KAFKA_VERSION/config/zookeeper.properties
./kafka_2.11-$KAFKA_VERSION/bin/kafka-server-start.sh -daemon kafka_2.11-$KAFKA_VERSION/config/server.properties
./apache-cassandra-$CASSANDRA_VERSION/bin/cassandra -p ./apache-cassandra-$CASSANDRA_VERSION/cassandra.pid

