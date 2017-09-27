#!/bin/bash
CWD=$(pwd)

apt-get -y update && apt-get -y install curl python-pip g++ cmake perl linux-tools-common linux-tools-$(uname -r) linux-image-extra-$(uname -r)
apt-get -y install software-properties-common
pip install flask

# Install Java.
echo oracle-java8-installer shared/accepted-oracle-license-v1-1 select true | debconf-set-selections && \
add-apt-repository -y ppa:webupd8team/java && \
apt-get update && \
apt-get install -y oracle-java8-installer && \
rm -rf /var/lib/apt/lists/* && \
rm -rf /var/cache/oracle-jdk8-installer

export JAVA_HOME="/usr/lib/jvm/java-8-oracle"
export JAVA_OPTIONS="-XX:+PreserveFramePointer -XX:InlineSmallCode=500"
export JAVA_OPTS=$JAVA_OPTIONS

curl -v -j -k -L -H "Cookie:oraclelicense=accept-securebackup-cookie" http://download.oracle.com/otn-pub/java/jce/8/jce_policy-8.zip  > jce_policy-8.zip
unzip jce_policy-8.zip
cp -f UnlimitedJCEPolicyJDK8/* $JAVA_HOME/jre/lib/security/

cmake . && make

# Turn off iptables
/etc/init.d/iptables save
/etc/init.d/iptables stop
