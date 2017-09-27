#!/bin/sh
set -e

PID=$1

if [ -z "$PERF_JAVA_TMP" ]; then
  PERF_JAVA_TMP=/tmp
fi

STACKS=$PERF_JAVA_TMP/out-$PID.stacks

COLLAPSED1=$PERF_JAVA_TMP/out-$PID.collapsed.1
COLLAPSED2=$PERF_JAVA_TMP/out-$PID.collapsed.2

PERF_DATA_FILE=$PERF_JAVA_TMP/perf-$PID.data

PERF_MAP_DIR=$(dirname $(readlink -f $0))/..

if [ -z "$PERF_RECORD_SECONDS" ]; then
  PERF_RECORD_SECONDS=30
fi

if [ -z "$PERF_RECORD_FREQ" ]; then
  PERF_RECORD_FREQ=99
fi

if [ -z "$FLAMEGRAPH_DIR" ]; then
  FLAMEGRAPH_DIR="$PERF_MAP_DIR/../flame-graph"
fi

if [ ! -x "$FLAMEGRAPH_DIR/stackcollapse-perf.pl" ]; then
  echo "FlameGraph executable not found at '$FLAMEGRAPH_DIR/stackcollapse-perf.pl'. Please set FLAMEGRAPH_DIR to the root of the clone of https://github.com/brendangregg/FlameGraph."
  exit
fi

if [ -z "$PERF_FLAME_OUTPUT" ]; then
  PERF_FLAME_OUTPUT=flamegraph-$PID.svg
fi

if [ -z "$PERF_FLAME_OPTS" ]; then
    PERF_FLAME_OPTS="--color=java --hash --fontsize=10 --title=growth(red)_reductions(blue)"
fi

sudo perf record -F $PERF_RECORD_FREQ -o $PERF_DATA_FILE -g -p $* -- sleep $PERF_RECORD_SECONDS
$PERF_MAP_DIR/bin/create-java-perf-map.sh $PID "$PERF_MAP_OPTIONS"
sudo perf script -i $PERF_DATA_FILE > $STACKS
$FLAMEGRAPH_DIR/stackcollapse-perf.pl $STACKS > $COLLAPSED1

sudo perf record -F $PERF_RECORD_FREQ -o $PERF_DATA_FILE -g -p $* -- sleep $PERF_RECORD_SECONDS
$PERF_MAP_DIR/bin/create-java-perf-map.sh $PID "$PERF_MAP_OPTIONS"
sudo perf script -i $PERF_DATA_FILE > $STACKS
$FLAMEGRAPH_DIR/stackcollapse-perf.pl $STACKS > $COLLAPSED2

$FLAMEGRAPH_DIR/difffolded.pl $COLLAPSED1 $COLLAPSED2 | $FLAMEGRAPH_DIR/flamegraph.pl $PERF_FLAME_OPTS > $PERF_FLAME_OUTPUT

echo "PERF_FLAME_OUTPUT=`readlink -f $PERF_FLAME_OUTPUT`"
