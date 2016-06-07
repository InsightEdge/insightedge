#!/usr/bin/env bash

# This is executed in Client container by ie-user

echo "Starting installation from Client container"

unzip -o /download/gigaspaces-insightedge-*.zip -d /download/
IE_HOME=$(ls /download/gigaspaces-insightedge*.zip | awk '{split($0,a,".zip"); print a[1]}')

# install master and slave
## override download zip command, used by insightedge.sh script
export ARTIFACT_DOWNLOAD_COMMAND="cp /download/gigaspaces-insightedge-*.zip ."
#$IE_HOME/sbin/insightedge.sh --mode remote-master --hosts master --user ie-user --key /home/ie-user/ie-user.pem --install --path /home/ie-user/ie --master master
#$IE_HOME/sbin/insightedge.sh --mode remote-slave --hosts slave --user ie-user --key /home/ie-user/ie-user.pem --install --path /home/ie-user/ie --master master






