#!/usr/bin/env bash
#
# Builds docker image. Used for dev purpose.
# Auto-test builds the image with maven plugin.
#

VER=1.1.0-SNAPSHOT

docker build -t insightedge-tests-cluster-install:$VER .