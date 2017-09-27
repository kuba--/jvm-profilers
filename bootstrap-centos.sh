#!/bin/bash

CWD=$(pwd)

yum -y update
yum -y install curl cmake make gcc gcc-c++ python-pip perf perl
pip install flask
curl -v -j -k -L -H "Cookie: oraclelicense=accept-securebackup-cookie" http://download.oracle.com/otn-pub/java/jdk/8u66-b17/jdk-8u66-linux-x64.rpm > jdk-8u66-linux-x64.rpm
yum -y localinstall jdk-8u66-linux-x64.rpm

export JAVA_HOME=/usr/java/jdk1.8.0_66
export JAVA_OPTIONS="-XX:+PreserveFramePointer -XX:InlineSmallCode=500"
export JAVA_OPTS=$JAVA_OPTIONS

curl -v -j -k -L -H "Cookie:oraclelicense=accept-securebackup-cookie" http://download.oracle.com/otn-pub/java/jce/8/jce_policy-8.zip  > jce_policy-8.zip
unzip jce_policy-8.zip
cp -f UnlimitedJCEPolicyJDK8/* $JAVA_HOME/jre/lib/security/

cmake . && make


# Turn off iptables
/etc/init.d/iptables save
/etc/init.d/iptables stop
