#!/usr/bin/env bash
#
# Stops and removes all containers
#

docker kill maven-image

docker rm maven-image
