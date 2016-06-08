#!/usr/bin/env bash
#
# Stops and removes all containers
#

docker kill slave1
docker kill slave2
docker kill master
docker kill client

docker rm slave1
docker rm slave2
docker rm master
docker rm client