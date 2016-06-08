#!/usr/bin/env bash
#
# Starts a cluster of containers and installs InsightEdge there.
#

VER=0.4.0-SNAPSHOT

if [ "$#" -ne 1 ]; then
    echo "Illegal number of parameters."
    echo "Usage: run.sh <dir of InsightEdge zip>"
    exit 1
fi

LOCAL_DOWNLOAD_DIR=$1

# Stop if anything is running
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
$DIR/stop.sh

# Run cluster containers
docker run --name master -P -d -v $LOCAL_DOWNLOAD_DIR:/download insightedge-tests-cluster-install:$VER
docker run --name slave -P -d -v $LOCAL_DOWNLOAD_DIR:/download insightedge-tests-cluster-install:$VER
docker run --name client -P -d --link master:master --link slave:slave -v $LOCAL_DOWNLOAD_DIR:/download insightedge-tests-cluster-install:$VER

MASTER_IP=$(docker inspect --format '{{ .NetworkSettings.IPAddress }}' master)
SLAVE_IP=$(docker inspect --format '{{ .NetworkSettings.IPAddress }}' slave)
CLIENT_IP=$(docker inspect --format '{{ .NetworkSettings.IPAddress }}' client)

echo "Master IP: $MASTER_IP"
echo "Slave IP: $SLAVE_IP"
echo "Client IP: $CLIENT_IP"

# Install & smoke test
docker exec --user ie-user client /home/ie-user/remote_install.sh


