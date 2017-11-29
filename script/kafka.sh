#!/usr/bin/env bash

# environment
KAFKA=kafka_2.11-1.0.0.tgz
KAFKA_MIRROR=http://apache.cs.utah.edu/kafka/1.0.0
ID=2
echo 'LC_ALL="en_US.UTF-8"' > /etc/default/locale

# establish ephemeral disk
umount /mnt
parted -s /dev/vdb mklabel gpt
parted -s /dev/vdb mkpart primary 2048s 100%
mkfs -t ext4 /dev/vdb1
sed -i 's/auto/ext4/' /etc/fstab
sed -i 's/vdb/vdb1/' /etc/fstab
mount /mnt
chmod 1777 /mnt

# install packages
apt-get update
apt-get install default-jdk -y
apt-get install zookeeperd -y

# setup zookeeper
sudo service zookeeper stop
mkdir -p /mnt/data/zookeeper
chown -R zookeeper:zookeeper /mnt/data/zookeeper
sed -i -e '/dataDir=/ s/=.*/=\/mnt\/data\/zookeeper/' /etc/zookeeper/conf/zoo.cfg
sed -i -e 's/#server.1=.*/server.1=10.50.50.83:2888:3888/' /etc/zookeeper/conf/zoo.cfg
sed -i -e 's/#server.2=.*/server.2=10.50.50.84:2888:3888/' /etc/zookeeper/conf/zoo.cfg
sed -i -e 's/#server.3=.*/server.3=10.50.50.85:2888:3888/' /etc/zookeeper/conf/zoo.cfg
echo $ID > /mnt/data/zookeeper/myid
sudo service zookeeper start

# setup kafka
mkdir /opt/kafka
mkdir /mnt/data/kafka
chown -R ubuntu:ubuntu /opt/kafka
chown -R ubuntu:ubuntu /mnt/data/kafka
wget $KAFKA_MIRROR/$KAFKA
sleep 2
tar -xvf $KAFKA -C /opt/kafka/
sed -i -e '/broker.id=/ s/=.*/=3/' /opt/kafka/$KAFKA/config/server.properties
sed -i -e '/log.dirs=/ s/=.*/=\/mnt\/data\/kafka/' /opt/kafka/$KAFKA/config/server.properties
