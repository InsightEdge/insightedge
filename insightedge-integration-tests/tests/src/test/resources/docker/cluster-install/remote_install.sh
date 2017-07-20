#!/usr/bin/env bash
set -x
#
# Starts InsightEdge installation on cluster and verifies installation with smoke test.
# Executed in Client container by ie-user.
#

if [ "$#" -ne 1 ]; then
    echo "Illegal number of parameters."
    echo "Usage: remote_install.sh <distribution edition>"
    exit 1
fi

EDITION=$1
echo "Starting installation from Client container, edition $EDITION"
set -e

mkdir ~/insightedge
unzip -o /download/gigaspaces-insightedge-*.zip -d ~/insightedge/
IE_HOME=$(ls -d -1 ~/insightedge/gigaspaces-insightedge-*)

### install master and 2 slaves

# override download zip command, used by insightedge.sh script
export ARTIFACT_DOWNLOAD_COMMAND="cp /download/gigaspaces-insightedge-*.zip ."

MASTER_IP=$MASTER_PORT_22_TCP_ADDR
SLAVE1_IP=$SLAVE1_PORT_22_TCP_ADDR
SLAVE2_IP=$SLAVE2_PORT_22_TCP_ADDR

if [[ "$EDITION" == "premium" ]]; then
    $IE_HOME/insightedge/sbin/insightedge.sh --mode remote-master --hosts $MASTER_IP --user ie-user --key /home/ie-user/ie-user.pem --install --path /home/ie-user/ie --master $MASTER_IP
    $IE_HOME/insightedge/sbin/insightedge.sh --mode remote-slave --hosts $SLAVE1_IP --user ie-user --key /home/ie-user/ie-user.pem --install --path /home/ie-user/ie --master $MASTER_IP
    $IE_HOME/insightedge/sbin/insightedge.sh --mode remote-slave --hosts $SLAVE2_IP --user ie-user --key /home/ie-user/ie-user.pem --install --path /home/ie-user/ie --master $MASTER_IP

    ### deploy

    # export nic address so we can get deployment notification from GSM via docker network
    export XAP_NIC_ADDRESS=$(hostname -i)
    export NIC_ADDR=$(hostname -i)
    $IE_HOME/insightedge/sbin/insightedge.sh --mode deploy --master $MASTER_IP
elif [[ "$EDITION" == "community" ]]; then
    $IE_HOME/insightedge/sbin/insightedge.sh --mode remote-master --hosts $MASTER_IP --user ie-user --key /home/ie-user/ie-user.pem --install --path /home/ie-user/ie --master $MASTER_IP
    $IE_HOME/insightedge/sbin/insightedge.sh --mode remote-slave --hosts "$SLAVE1_IP,$SLAVE2_IP" --user ie-user --key /home/ie-user/ie-user.pem --install --path /home/ie-user/ie --master $MASTER_IP
else
    echo "ERROR. Couldn't parse edition parameter: $EDITION"
    exit 1
fi

### smoke test
$IE_HOME/insightedge/bin/insightedge-submit --class org.insightedge.examples.basic.SaveRdd --master spark://$MASTER_IP:7077 $IE_HOME/insightedge/quickstart/scala/insightedge-examples.jar spark://$MASTER_IP:7077 insightedge-space insightedge $MASTER_IP:4174






