#!/bin/bash

/opt/gigaspaces-insightedge/insightedge/sbin/insightedge.sh demo 2>&1 > /opt/gigaspaces-insightedge/insightedge/logs/bootstrap.log


if [[ $1 == "-d" ]]; then
  while true; do sleep 1000; done
fi

if [[ $1 == "-bash" ]]; then
  /bin/bash
fi