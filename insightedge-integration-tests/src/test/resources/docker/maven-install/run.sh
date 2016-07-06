#!/usr/bin/env bash
#
# Starts a cluster of containers and installs InsightEdge there.
#

#TODO pass as param?
VER=0.4.0-SNAPSHOT

if [ "$#" -ne 2 ]; then
    echo "Illegal number of parameters."
    echo "Usage: run.sh <dir with InsightEdge zip> <distribution edition>"
    exit 1
fi

LOCAL_DOWNLOAD_DIR=$1
EDITION=$2

# Stop if anything is running
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
$DIR/stop.sh

#TODO cleanup
sleep 5
echo "Run image"
echo "docker run --name maven-image -P -d -v $LOCAL_DOWNLOAD_DIR:/download insightedge-maven-install:$VER"
docker run --name maven-image -P -d -v $LOCAL_DOWNLOAD_DIR:/download insightedge-maven-install:$VER
echo "Run image DONE"
sleep 5
docker ps

echo "Local download dir: $LOCAL_DOWNLOAD_DIR"

# Install & smoke test
echo "Run maven test"
docker exec --user ie-user maven-image /home/ie-user/maven-test.sh
echo "Run maven test DONE"


