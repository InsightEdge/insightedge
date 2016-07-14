#!/usr/bin/env bash
#
# Stops and removes all containers
#

docker kill maven-intall-libs-test-image

docker rm maven-intall-libs-test-image
