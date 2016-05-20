#!/usr/bin/env bash



# INSTALLING GAEA ------------------------------------

# install jdk
sudo apt-get install openjdk-8-jdk
sudo apt-get install zip unzip ruby

# install maven
mkdir -p ~/bin && curl -s https://bitbucket.org/mjensen/mvnvm/raw/master/mvn > ~/bin/mvn && chmod 0755 ~/bin/mvn
export PATH=~/bin:$PATH

# install sbt
echo "deb https://dl.bintray.com/sbt/debian /" | sudo tee -a /etc/apt/sources.list.d/sbt.list
sudo apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv 642AC823
sudo apt-get update
sudo apt-get install sbt

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
