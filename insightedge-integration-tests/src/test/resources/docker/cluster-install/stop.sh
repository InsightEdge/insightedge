#!/usr/bin/env bash
#
# Stops and removes all containers
#

docker kill slave
docker kill master
docker kill client

docker rm master
docker rm slave
docker rm client