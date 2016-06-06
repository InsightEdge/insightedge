#!/usr/bin/env bash

LOCAL_DOWNLOAD_DIR=/home/fe2s/Soft
IE_LOCAL_HOME=/home/fe2s/Soft/gigaspaces-insightedge

docker kill slave
docker kill master

docker rm master
docker rm slave

docker run --name master -P -d -v $LOCAL_DOWNLOAD_DIR:/download insightedge-integration-test-cluster
docker run --name slave -P -d -v $LOCAL_DOWNLOAD_DIR:/download insightedge-integration-test-cluster

MASTER_IP=$(docker inspect --format '{{ .NetworkSettings.IPAddress }}' master)
SLAVE_IP=$(docker inspect --format '{{ .NetworkSettings.IPAddress }}' slave)

echo "Master IP: $MASTER_IP"
echo "Slave IP: $SLAVE_IP"

MASTER_SSH_PORT=$(docker port master 22 | awk '{split($0,a,":"); print a[2]}')
SLAVE_SSH_PORT=$(docker port slave 22 | awk '{split($0,a,":"); print a[2]}')

echo "Master SSH port: $MASTER_SSH_PORT"
echo "Slave SSH port: $SLAVE_SSH_PORT"

# override download zip command
export ARTIFACT_DOWNLOAD_COMMAND="cp /download/gigaspaces-insightedge-*.zip ."

# TODO: change _dev.sh
$IE_LOCAL_HOME/sbin/insightedge_dev.sh --mode remote-master --hosts 172.17.0.1 --user ie-user --key ./ie-user.pem --install --path /home/ie-user/ie --master $MASTER_IP --ssh-port $MASTER_SSH_PORT
$IE_LOCAL_HOME/sbin/insightedge_dev.sh --mode remote-slave --hosts 172.17.0.1 --user ie-user --key ./ie-user.pem --install --path /home/ie-user/ie --master $MASTER_IP --ssh-port $SLAVE_SSH_PORT

# TODO:
#[fe2s@epplkraw0306t1 sbin]$ ./insightedge_dev.sh --mode deploy --master 172.17.0.1 --locator 0.0.0.0:33219





