#!/bin/bash
mkdir -p target

echo "TAG_NAME = ${TAG_NAME}"
echo "IE_SHA = ${IE_SHA}"


if [ ! -z "${TAG_NAME}" ]
then
    echo InsightEdge:https://github.com/InsightEdge/insightedge/tree/${TAG_NAME} > target/metadata.txt
    exit 0
fi

if [ -z "$IE_SHA" ]
then
  SHA=`git rev-parse HEAD`
  echo InsightEdge:https://github.com/InsightEdge/insightedge/commit/${SHA} > target/metadata.txt
else
  SHA="$IE_SHA"
  echo InsightEdge:https://github.com/InsightEdge/insightedge/tree/${SHA} > target/metadata.txt
fi
