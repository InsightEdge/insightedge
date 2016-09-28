#!/bin/bash
set -x
echo "executing InsightEdge Integration test: $1, edition: ${DIST_EDITION}"
JOB_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
echo "Job DIR: ${JOB_DIR}"
echo start run-insightedge-integration.sh
echo `env`
mvn -f ${JOB_DIR}/insightedge-integration-tests/pom.xml -P run-integration-tests-${DIST_EDITION} -P run-external -DwildcardSuites=$1 -Ddist.dir=${JOB_DIR}/build verify
EXIT_CODE=$?
exit $EXIT_CODE
