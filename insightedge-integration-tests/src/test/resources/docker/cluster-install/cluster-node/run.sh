#!/usr/bin/env bash

if [ "$#" -ne 3 ]; then
    echo "Illegal number of parameters."
    echo "Usage: run.sh <dir of InsightEdge zip> <dir of unpacked InsightEdge> <path to ie-user.pem>"
    exit 1
fi


LOCAL_DOWNLOAD_DIR=$1
LOCAL_IE_HOME=$2


docker kill slave
docker kill master

docker rm master
docker rm slave

docker run --name master -P -d -v $LOCAL_DOWNLOAD_DIR:/download insightedge-tests-cluster-install
docker run --name slave -P -d -v $LOCAL_DOWNLOAD_DIR:/download insightedge-tests-cluster-install

MASTER_IP=$(docker inspect --format '{{ .NetworkSettings.IPAddress }}' master)
SLAVE_IP=$(docker inspect --format '{{ .NetworkSettings.IPAddress }}' slave)

echo "Master IP: $MASTER_IP"
echo "Slave IP: $SLAVE_IP"

# override download zip command
export ARTIFACT_DOWNLOAD_COMMAND="cp /download/gigaspaces-insightedge-*.zip ."

# TODO: change _dev.sh
# install master and slave
$LOCAL_IE_HOME/sbin/insightedge_dev.sh --mode remote-master --hosts $MASTER_IP --user ie-user --key ./ie-user.pem --install --path /home/ie-user/ie --master $MASTER_IP
$LOCAL_IE_HOME/sbin/insightedge_dev.sh --mode remote-slave --hosts $SLAVE_IP --user ie-user --key ./ie-user.pem --install --path /home/ie-user/ie --master $MASTER_IP

# deploy
# export nic address so we can get deployment notification from GSM via docker network
export XAP_NIC_ADDRESS="172.17.0.1"
export NIC_ADDR="172.17.0.1"
$LOCAL_IE_HOME/sbin/insightedge_dev.sh --mode deploy --master $MASTER_IP





