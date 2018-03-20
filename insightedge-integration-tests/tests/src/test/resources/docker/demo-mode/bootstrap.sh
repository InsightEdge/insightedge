#!/bin/bash

/opt/gigaspaces-insightedge/bin/insightedge demo 2>&1 > /opt/gigaspaces-insightedge/logs/bootstrap.log


if [[ $1 == "-d" ]]; then
  while true; do sleep 1000; done
fi

if [[ $1 == "-bash" ]]; then
  /bin/bash
fi