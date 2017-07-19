#!/bin/bash
set -x -e
echo "executing InsightEdge Integration test: $1, edition: ${DIST_EDITION}"
JOB_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
echo "Job DIR: ${JOB_DIR}"
echo start run-insightedge-integration.sh
TEST_FOLDER=$PWD
echo "TEST_FOLDER: ${TEST_FOLDER}"

echo "ENV:"
echo `env`
chmod -R 755 ${JOB_DIR}/build/bin/*
chmod -R 755 ${JOB_DIR}/build/insightedge/bin/*
chmod -R 755 ${JOB_DIR}/build/insightedge/sbin/*
chmod -R 755 ${JOB_DIR}/build/insightedge/spark/bin/*
chmod -R 755 ${JOB_DIR}/build/insightedge/spark/sbin/*
chmod -R 755 ${JOB_DIR}/build/insightedge/spark/conf/*

cp ${JOB_DIR}/build/insightedge/conf/spark-defaults.conf.template ${JOB_DIR}/build/insightedge/conf/spark-defaults.conf
echo "spark.eventLog.enabled=true" > ${JOB_DIR}/build/insightedge/conf/spark-defaults.conf


mvn -f ${JOB_DIR}/insightedge-integration-tests/tests/pom.xml -P run-integration-tests-${DIST_EDITION} -P run-external -DwildcardSuites=$1 -Ddist.dir=${JOB_DIR}/build -Dgit.branch=${GIT_BRANCH} -Dtest.folder=${TEST_FOLDER} verify
EXIT_CODE=$?
exit $EXIT_CODE
