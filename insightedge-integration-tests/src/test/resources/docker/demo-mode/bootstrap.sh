#!/bin/bash

/opt/gigaspaces-insightedge/sbin/insightedge.sh --mode demo

if [[ $1 == "-d" ]]; then
  while true; do sleep 1000; done
fi

if [[ $1 == "-bash" ]]; then
  /bin/bash
fi