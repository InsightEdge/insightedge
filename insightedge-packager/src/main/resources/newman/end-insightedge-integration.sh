#!/bin/bash
echo "tearing down insightedge-integration test"
#stop and remove all dockers
docker stop $(docker ps -a -q)
docker rm $(docker ps -a -q)