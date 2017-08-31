#!/bin/bash
set -x
JOB_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
echo "Job DIR: ${JOB_DIR}"
TEST_FOLDER=$PWD
echo "TEST_FOLDER: ${TEST_FOLDER}"

echo "Copying logs to logs directory"
cp -R ${JOB_DIR}/build/insightedge/spark/logs/* ${TEST_FOLDER}/output/
cp -R ${JOB_DIR}/build/insightedge/zeppelin/logs/* ${TEST_FOLDER}/output/

echo "tearing down insightedge-integration test"
#stop and remove all dockers
docker rm -f $(docker ps -a -q)