#!/usr/bin/env bash
#
# Builds docker image. Used for dev purpose.
# Auto-test builds the image with maven plugin.
#

VER=0.4.0-SNAPSHOT

docker build -t insightedge-tests-maven-install:$VER .