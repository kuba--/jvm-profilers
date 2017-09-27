#!/bin/bash
set -e

CUR_DIR=`pwd`
PID=$1
OPTIONS=$2

BUILD_DIR=$(dirname $(readlink -f $0))/../../build
AGENT_JAR="agent.jar"

AGENT_JAR_PATH=$BUILD_DIR/$AGENT_JAR
PERF_MAP_FILE=/tmp/perf-$PID.map

if [ -z "$JAVA_HOME" ]; then
  JAVA_HOME=/usr/lib/jvm/default-java
fi
[ -d "$JAVA_HOME" ] || JAVA_HOME=/etc/alternatives/java_sdk
[ -d "$JAVA_HOME" ] || (echo "JAVA_HOME directory at '$JAVA_HOME' does not exist." && false)

sudo rm $PERF_MAP_FILE -f
(cd $BUILD_DIR && java -cp $AGENT_JAR_PATH:$JAVA_HOME/lib/tools.jar Attach $PID libperfmap.so "$OPTIONS")
sudo chown root:root $PERF_MAP_FILE
