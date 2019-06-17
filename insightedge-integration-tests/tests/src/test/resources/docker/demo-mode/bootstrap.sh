#!/bin/bash

/opt/gigaspaces-insightedge/bin/gs demo > /opt/gigaspaces-insightedge/logs/bootstrap.log 2>&1


if [[ $1 == "-d" ]]; then
  while true; do sleep 1000; done
fi

if [[ $1 == "-bash" ]]; then
  /bin/bash
fi