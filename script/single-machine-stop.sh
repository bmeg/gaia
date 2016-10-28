#!/bin/bash

KAFKA_VERSION=0.10.0.1
CASSANDRA_VERSION=3.7
APACHE_MIRROR=www.gtlib.gatech.edu

BDIR="$(cd `dirname $0 && cd ../`; pwd)"
cd $BDIR

./kafka_2.11-$KAFKA_VERSION/bin/kafka-server-stop.sh 
./kafka_2.11-$KAFKA_VERSION/bin/zookeeper-server-stop.sh 
kill `cat ./apache-cassandra-$CASSANDRA_VERSION/cassandra.pid`