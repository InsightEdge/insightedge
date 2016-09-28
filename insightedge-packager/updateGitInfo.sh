#!/bin/bash
mkdir -p target
if [ -z "$INSIGHTEDGE_SHA" ]
then
  SHA=`git rev-parse HEAD`
  echo InsightEdge:https://github.com/InsightEdge/insightedge/commit/${SHA} > target/metadata.txt
else
  SHA="$INSIGHTEDGE_SHA"
  echo InsightEdge:https://github.com/InsightEdge/insightedge/tree/${SHA} > target/metadata.txt
fi
