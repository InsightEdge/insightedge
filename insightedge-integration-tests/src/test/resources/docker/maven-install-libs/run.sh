#!/usr/bin/env bash
#
# Starts a docker image with pre-installed Maven/Sbt and runs tests.
#

if [ "$#" -ne 3 ]; then
    echo "Illegal number of parameters."
    echo "Usage: run.sh <dir with InsightEdge zip> <distribution version>"
    exit 1
fi

LOCAL_DOWNLOAD_DIR=$1
VERSION=$2
echo "Local download dir: $LOCAL_DOWNLOAD_DIR"
echo "Version: $VERSION"

# Stop if anything is running
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
$DIR/stop.sh

docker run --name maven-install-libs-test-image -P -d -v $LOCAL_DOWNLOAD_DIR:/download insightedge-tests-maven-install-libs:$VERSION

docker exec --user ie-user maven-install-libs-test-image /home/ie-user/maven-install-libs-test.sh


