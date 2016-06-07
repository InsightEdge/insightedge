#!/usr/bin/env bash

if [ "$#" -ne 1 ]; then
    echo "Illegal number of parameters."
    echo "Usage: run.sh <dir of InsightEdge zip>"
    exit 1
fi

LOCAL_DOWNLOAD_DIR=$1

docker kill slave
docker kill master
docker kill client

docker rm master
docker rm slave
docker rm client

docker run --name master -P -d -v $LOCAL_DOWNLOAD_DIR:/download insightedge-tests-cluster-node-install
docker run --name slave -P -d -v $LOCAL_DOWNLOAD_DIR:/download insightedge-tests-cluster-node-install
docker run --name client -P -d --link master:master --link slave:slave -v $LOCAL_DOWNLOAD_DIR:/download insightedge-tests-cluster-node-install

MASTER_IP=$(docker inspect --format '{{ .NetworkSettings.IPAddress }}' master)
SLAVE_IP=$(docker inspect --format '{{ .NetworkSettings.IPAddress }}' slave)
CLIENT_IP=$(docker inspect --format '{{ .NetworkSettings.IPAddress }}' client)

echo "Master IP: $MASTER_IP"
echo "Slave IP: $SLAVE_IP"
echo "Client IP: $CLIENT_IP"

docker exec --user ie-user -it client /home/ie-user/remote_install.sh

# override download zip command
#export ARTIFACT_DOWNLOAD_COMMAND="cp /download/gigaspaces-insightedge-*.zip ."

# TODO: change _dev.sh
# install master and slave
#$LOCAL_IE_HOME/sbin/insightedge_dev.sh --mode remote-master --hosts $MASTER_IP --user ie-user --key $PATH_TO_PEM_FILE --install --path /home/ie-user/ie --master $MASTER_IP
#$LOCAL_IE_HOME/sbin/insightedge_dev.sh --mode remote-slave --hosts $SLAVE_IP --user ie-user --key $PATH_TO_PEM_FILE --install --path /home/ie-user/ie --master $MASTER_IP

# deploy
# export nic address so we can get deployment notification from GSM via docker network
#export XAP_NIC_ADDRESS="172.17.0.1"
#export NIC_ADDR="172.17.0.1"
#$LOCAL_IE_HOME/sbin/insightedge_dev.sh --mode deploy --master $MASTER_IP





