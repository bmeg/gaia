#!/usr/bin/env bash



# INSTALLING GAEA ------------------------------------

# install jdk
sudo apt-get update
sudo apt-get --force-yes install openjdk-8-jdk
sudo apt-get --force-yes install zip unzip ruby

# install maven
mkdir -p ~/bin && curl -s https://bitbucket.org/mjensen/mvnvm/raw/master/mvn > ~/bin/mvn && chmod 0755 ~/bin/mvn
export PATH=~/bin:$PATH

# install sbt
echo "deb https://dl.bintray.com/sbt/debian /" | sudo tee -a /etc/apt/sources.list.d/sbt.list
sudo apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv 642AC823
sudo apt-get --force-yes install sbt

# configure git
git config --global alias.co checkout
git config --global alias.ci commit
git config --global alias.st status
git config --global alias.br branch

# install titan
git clone https://github.com/thinkaurelius/titan.git
cd titan
echo "mvn_version=3.0.5" > mvnvm.properties
git checkout titan11
mvn clean install -DskipTests=true -Paurelius-release -Dgpg.skip=true
cd ..

# install gaea
git clone https://github.com/bmeg/gaea.git


# SBT CONFIGURATION -----------------------------------------

sudo nano /usr/share/sbt-launcher-packaging/conf/sbtopts
-J-XX:-UseConcMarkSweepGC
-J-Xmx4G


# INSTALLING CASSANDRA -----------------------------------

echo "deb http://www.apache.org/dist/cassandra/debian 22x main" | sudo tee -a /etc/apt/sources.list.d/cassandra.sources.list
echo "deb-src http://www.apache.org/dist/cassandra/debian 22x main" | sudo tee -a /etc/apt/sources.list.d/cassandra.sources.list

gpg --keyserver pgp.mit.edu --recv-keys F758CE318D77295D
gpg --export --armor F758CE318D77295D | sudo apt-key add -

gpg --keyserver pgp.mit.edu --recv-keys 2B5C1B00
gpg --export --armor 2B5C1B00 | sudo apt-key add -

gpg --keyserver pgp.mit.edu --recv-keys 0353B12C
gpg --export --armor 0353B12C | sudo apt-key add -


# old way
echo "deb http://debian.datastax.com/datastax-ddc 3.version_number main" | sudo tee -a /etc/apt/sources.list.d/cassandra.sources.list
curl -L https://debian.datastax.com/debian/repo_key | sudo apt-key add -
sudo apt-get update
sudo apt-get install openjdk-8-jdk
sudo apt-get install datastax-ddc


# CASSANDRA CONFIGURATION ------------------------------------

seed_provider:
    parameters:
        seeds: "10.104.0.5"

listen_interface: eth0
start_rpc: true
rpc_address: 0.0.0.0
broadcast_rpc_address: 10.104.0.5


# SWIFT OBJECT STORE -----------------------------------------

# put this script in ~/.swift

#!/usr/bin/env bash

unset -v OS_SERVICE_TOKEN OS_USERNAME OS_PASSWORD
unset -v OS_AUTH_URL OS_TENANT_NAME OS_REGION_NAME
export OS_USERNAME=username
export OS_PASSWORD=password
export OS_TENANT_NAME=tenant
export PS1='[\u SWIFT \W]\$ '
export OS_AUTH_URL=http://exastack-00.ohsu.edu:5000/v2.0
export OS_REGION_NAME=RegionOne

# Then execute these commands:

sudo apt-get install python-pip
sudo pip install python-swiftclient
sudo pip install python-keystoneclient
source ~/.swift


# INSTALLING DOCKER ---------------------------------------

sudo apt-get install apt-transport-https ca-certificates
sudo apt-key adv --keyserver hkp://p80.pool.sks-keyservers.net:80 --recv-keys 58118E89F3A912897C070ADBF76221572C52609D
sudo echo "deb https://apt.dockerproject.org/repo ubuntu-vivid main" > /etc/apt/sources.list.d/docker.list
sudo apt-get update
apt-cache policy docker-engine
sudo apt-get install docker-engine
sudo service docker start



# INSTALLING KAFKA ----------------------------------------------

wget http://mirror.symnds.com/software/Apache/kafka/0.10.0.0/kafka_2.11-0.10.0.0.tgz
tar xzvf kafka_2.11-0.10.0.0.tgz
cd kafka_2.11-0.10.0.0
tmux
bin/zookeeper-server-start.sh config/zookeeper.properties
C-b c
bin/kafka-server-start.sh config/server.properties
C-b c
bin/kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 1 --partitions 1 --topic test
bin/kafka-topics.sh --list --zookeeper localhost:2181
bin/kafka-console-producer.sh --broker-list localhost:9092 --topic test
C-b c
bin/kafka-console-consumer.sh --zookeeper localhost:2181 --topic test --from-beginning

# example zookeeper config ********

# the directory where the snapshot is stored.
dataDir=/var/zookeeper/data
# the port at which the clients will connect
clientPort=2181
# disable the per-ip limit on the number of connections since this is a non-production config
maxClientCnxns=0
server.1=10.40.40.24:2888:3888
server.2=10.40.40.23:2888:3888
server.3=10.40.40.25:2888:3888
initLimit=5
syncLimit=2

# example kafka config *********

broker.id=1
advertised.listeners=PLAINTEXT://10.96.11.91:9092
zookeeper.connect=10.40.40.23:2181,10.40.40.24:2181,10.40.40.25:2181



# NGINX for BMEG.IO

upstream gaea {
    server localhost:11223;
}

upstream staging {
    server 10.104.0.8:11223;
}

upstream observation {
    server 10.104.0.9:3100;
}

upstream psychic {
    server 10.104.0.9:8080;
}

server {
    listen 80;
    server_name bmeg.io;

    root /home/ubuntu/gaea/resources/public;

    index static/main.html;

    location / {
        try_files $uri $uri/main.html /static/$uri /static/main.html =404;
    }

    location /sample-psychic {
        proxy_pass http://psychic;
    }

    location /observation-deck {
        proxy_pass http://observation;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
    }

    location /gaea {
	proxy_pass http://gaea;
    }
}

server {
    listen 80;
    server_name staging.bmeg.io;

    location / {
        proxy_pass http://staging;
    }
}



# EXASTACK CONFIG ---------------------------------------------

#cloud-config
users:
  - name: spanglry
    gecos: Ryan Spangler
    sudo: ['ALL=(ALL) NOPASSWD:ALL']
    shell: /bin/bash
    groups: sudo
    ssh-authorized-keys:
      - ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAACAQDGndX51JpObA9/MC+p60s/9S54j0OjqlO7f/Tz9U2GsnDmMki8XpZOw0klJG7YFniIWhLKq1caAEk1/OyLjB8693h2Rv7HAdnNnUba5YmnsI9sgdHqZ5iMSD5qPNQDcmepvPt0k/UO0PzuSKeO4CmnWfZUIy0pTvWrP6Q4rXQzT6VlcxxlAFlwPNmZ6k0Oezf9QhYFBBHU3m0fOMbLFoiSv+k833TuawZYoM/TcIfwVo5lhiJqb6+mguTvjKkvIr1a6zJrTOK6lkPoZaTnME4Nr3P6rwdl5PbjOlS8g1tBHOarHmScuOmfnLlzQi+DRf10ZPBQW/ncppaON79wleVpUYuccKv6dffIeZQ3oqvGKJxLXRzE5gy5Zy8DEXsYapMwCU+KlVkHTDnSWnwznVG8jDs1KdWBj2hLZu3L3xNAYSpEbn8NrcY1bp1s8KziFN24efxbJmABnINYguMNajc/lCHwRJC1dGE/keYxuGgvpSuQUB8+XkpW7wv7u7GoTQ0yt7BxyHttKmuowOMf44N+cYjpQIC0ftO6Xet65UqCH+VpQ/AbHWIsdYsl3gISGYoE3rUYmZsuYed/l6V4CndAqpA5GBeQJeK+Jp61vFQTJ94uXQHObQjU8HeUTvqPc4kkzTQQ/2Jx0/HLq0Us/6EPrkfE3wOLFvaO/ByYoeT9DQ== ryan.spangler@gmail.com

package_upgrade: true
# end of file




# Erich's cloud disk partition -----------
# -------------------------------------

# #cloud-config
mounts:
  - [ ephemeral0, /data, auto, "defaults,noexec" ]

runcmd:
  - umount /data
  - parted -s /dev/vdb mklabel gpt
  - parted -s /dev/vdb mkpart primary 2048s 100%
  - mkfs -t ext4 /dev/vdb1
  - sed -i 's/auto/ext4/' /etc/fstab
  - sed -i 's/vdb/vdb1/' /etc/fstab
  - mount /data
  - chmod 1777 /data
