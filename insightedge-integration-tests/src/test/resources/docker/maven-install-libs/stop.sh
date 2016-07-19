#!/usr/bin/env bash
#
# Stops and removes all containers
#

docker kill maven-install-libs-test-image

docker rm maven-install-libs-test-image
