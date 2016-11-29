#!/bin/bash
echo "tearing down insightedge-integration test"
#stop and remove all dockers
docker rm -f $(docker ps -a -q)