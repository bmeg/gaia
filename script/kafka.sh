apt-get update
apt-get install default-jdk -y
apt-get install zookeeperd -y

mkdir /opt/kafka
wget http://apache.cs.utah.edu/kafka/1.0.0/kafka_2.11-1.0.0.tgz 
tar -xvf kafka_2.10-0.10.0.1.tgz -C /opt/kafka/
