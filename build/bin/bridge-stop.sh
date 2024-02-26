#!/bin/bash

if [ $# -lt 1 ];
then
  echo "USAGE: $0 servicename"
  exit 1
fi

NAME=$1

SIGNAL=${SIGNAL:-TERM}

PIDS=$(ps -ef | grep $NAME | grep java | grep -v grep | awk '{print $2}')

if [ -z "$PIDS" ]; then
  echo "No $NAME to stop"
  exit 1
else
  echo "Find $NAME process $PIDS, and stopping it"
  kill -s $SIGNAL $PIDS
fi

ret=1
for i in {1..300}
do
    ps -ef | grep $NAME | grep java | grep -v grep >/dev/null 2>&1
    if [ $? -ne 0 ]; then
        echo "$NAME process $PIDS was stopped"
        ret=0
        break
    fi
    sleep 1
done

if [ $ret -eq 1 ]; then
    echo "Wait process stop timeout: $NAME does not stop for more than 300s!"
fi
exit $ret