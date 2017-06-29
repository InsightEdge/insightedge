#!/bin/bash
set -x
OUTPUT_FILE=$( dirname "${BASH_SOURCE[0]}" )/env.sh

function validate {
    name=$1
    val=$(eval echo \$$1)
    if [ "$val" == "" ]; then echo "Validate failed - $name is not set"; exit 1; fi
}

function setIEFinalBuildNumber {
    validate "MODE"
    validate "IE_BUILD_NUMBER"
    validate "RUNNING_BUILD_NUMBER"
    if [ "$MODE" == "NIGHTLY" ]; then
        IE_FINAL_BUILD_NUMBER="$IE_BUILD_NUMBER-$RUNNING_BUILD_NUMBER"
    else
        IE_FINAL_BUILD_NUMBER="$IE_BUILD_NUMBER"
    fi
}

function setIEBuildVersion {
    validate "$IE_VERSION"
    validate "$MILESTONE"
    validate "$IE_FINAL_BUILD_NUMBER"
    FINAL_IE_BUILD_VERSION="$IE_VERSION-$MILESTONE-$IE_FINAL_BUILD_NUMBER"
}
function setIEMavenVersion {
    validate "MODE"
    validate "IE_VERSION"
    validate "MILESTONE"
    validate "IE_FINAL_BUILD_NUMBER"
    if [ "$MODE" == "NIGHTLY" ]; then
        IE_MAVEN_VERSION="$IE_VERSION-$MILESTONE-$IE_FINAL_BUILD_NUMBER"
    elif [ "$MODE" == "MILESTONE" ]; then
        IE_MAVEN_VERSION="$IE_VERSION-$MILESTONE"
    elif [ "$MODE" == "GA" ]; then
        IE_MAVEN_VERSION="$IE_VERSION"
    else
        echo "Unknown mode [$MODE]"
        exit 1
    fi
}

if [ -e "${OUTPUT_FILE}" ]; then
    rm -rf ${OUTPUT_FILE}
fi

function store {
    val=$(eval echo "\$$1")
    if [ "$val" == "" ]; then
        echo "Cannot store $1 - not set"
        exit 1
    fi
    echo "$1=$val" >> ${OUTPUT_FILE}
}

#BRANCH=master
#IE_VERSION=1.1.0
#MILESTONE=ga
#XAP_RELEASE_VERSION=12.1.0
#XAP_OPEN_URL="1"
#XAP_PREMIUM_URL="x"
#TAG_NAME="1.0.0-ga-NIGHTLY"
#storageHost="1"
#storagePort="1"
#storageUser="1"
#IE_BUILD_NUMBER="11000"
#M2="..."
#OVERRIDE_EXISTING_TAG="true"
#PERFORM_FULL_M2_CLEAN="false"
#MODE="NIGHTLY"
#NEWMAN_TAGS="INSIGHTEDGE"
#RUNNING_BUILD_NUMBER="10"

setIEFinalBuildNumber
setIEFinalBuildVersion
setIEMavenVersion

store "IE_FINAL_BUILD_NUMBER"
store "FINAL_IE_BUILD_VERSION"
store "IE_MAVEN_VERSION"
store "BRANCH"
store "IE_VERSION"
store "MILESTONE"
store "XAP_RELEASE_VERSION"
store "XAP_OPEN_URL"
store "XAP_PREMIUM_URL"
store "TAG_NAME"
store "storageHost"
store "storagePort"
store "storageUser"
store "M2"
store "OVERRIDE_EXISTING_TAG"
store "PERFORM_FULL_M2_CLEAN"
store "MODE"
store "NEWMAN_TAGS"

cat $OUTPUT_FILE