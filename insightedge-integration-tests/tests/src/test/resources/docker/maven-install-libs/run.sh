#!/usr/bin/env bash
set -x
#
# Starts a docker image with pre-installed Maven/Sbt and runs tests.
#

if [ "$#" -ne 4 ]; then
    echo "Illegal number of parameters."
    echo "Usage: run.sh <dir with InsightEdge zip> <distribution version> <Git branch>  <Build Option>"
    exit 1
fi

LOCAL_DOWNLOAD_DIR=$1
IE_VERSION=$2
BRANCH=$3
MVN_OR_SBT=$4
echo "Local download dir: $LOCAL_DOWNLOAD_DIR"
echo "IE Version: $IE_VERSION"
echo "Git branch: $BRANCH"
echo "Build option: $MVN_OR_SBT"

# Stop if anything is running
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
$DIR/stop.sh

#validate test not getting error after trying to stop all containers
set -e
docker run --name maven-install-libs-test-image -P -d -v $LOCAL_DOWNLOAD_DIR:/download insightedge-tests-maven-install-libs

docker exec --user ie-user maven-install-libs-test-image /home/ie-user/install-libs-test.sh $BRANCH $MVN_OR_SBT


