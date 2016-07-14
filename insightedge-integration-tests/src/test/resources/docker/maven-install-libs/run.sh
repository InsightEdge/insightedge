#!/usr/bin/env bash
#
# Starts a cluster of containers and installs InsightEdge there.
#

if [ "$#" -ne 3 ]; then
    echo "Illegal number of parameters."
    echo "Usage: run.sh <dir with InsightEdge zip> <distribution edition> <distribution version>"
    exit 1
fi

LOCAL_DOWNLOAD_DIR=$1
EDITION=$2
VERSION=$3
echo "Local download dir: $LOCAL_DOWNLOAD_DIR"
echo "Edition: $EDITION"
echo "Version: $VERSION"

# Stop if anything is running
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
$DIR/stop.sh

docker run --name maven-intall-libs-test-image -P -d -v $LOCAL_DOWNLOAD_DIR:/download insightedge-tests-maven-install-libs:$VERSION

docker exec maven-intall-libs-test-image /etc/maven-install-libs-test.sh
#docker exec --user ie-user maven-intall-libs-test-image /home/ie-user/maven-install-libs-test.sh


