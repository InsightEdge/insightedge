#!/bin/bash

/opt/gigaspaces-insightedge/insightedge/sbin/insightedge.sh --mode demo 2>&1 > /opt/bootstrap.log

if [[ $1 == "-d" ]]; then
  while true; do sleep 1000; done
fi

if [[ $1 == "-bash" ]]; then
  /bin/bash
fi