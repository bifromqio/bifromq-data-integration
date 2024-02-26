#!/bin/bash

BASE_DIR=$(
  cd "$(dirname "$0")"
  pwd
)/..
export LOG_DIR=$BASE_DIR/logs

if [ $# -lt 1 ]; then
  echo "USAGE: $0 {start|stop|restart} [-fg]"
  exit 1
fi

COMMAND=$1
shift

if [ $COMMAND = "start" ]; then
  exec "$BASE_DIR/bin/bridge-start.sh" -c bifromq.bridge.service.Starter -f config.yml "$@"
elif [ $COMMAND = "stop" ]; then
  exec "$BASE_DIR/bin/bridge-stop.sh" bifromq.bridge.service.Starter
elif [ $COMMAND = "restart" ]; then
  sh "$BASE_DIR/bin/bridge-stop.sh" bifromq.bridge.service.Starter
  "$BASE_DIR/bin/bridge-start.sh" -c bifromq.bridge.service.Starter -f config.yml "$@"
fi