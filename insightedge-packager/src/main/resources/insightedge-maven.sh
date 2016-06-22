#!/bin/bash

# This scripts installs InsightEdge artifacts(jars) to local maven repository

INSIGHTEDGE_VER=0.4.0-SNAPSHOT

if [ -z "${INSIGHTEDGE_HOME}" ]; then
  INSIGHTEDGE_HOME="$(cd "`dirname "$0"`"/..; pwd)"
fi

XAP_HOME=${INSIGHTEDGE_HOME}/datagrid
echo "Data Grid home $XAP_HOME"

if [ -z "${JAVA_HOME}" ]; then
	export JAVACMD=java
else
	export JAVACMD="${JAVA_HOME}/bin/java"
fi

GS_JARS=${XAP_HOME}/lib/required/*

DATA_GRID_VERSION=`${JAVACMD} -cp "${GS_JARS}" org.openspaces.maven.support.OutputVersion XAP`

echo "Installing Data Grid $DATA_GRID_VERSION artifacts"

mvn install:install-file \
 -DgroupId=com.gigaspaces \
 -DcreateChecksum=true \
 -DartifactId=gs-runtime \
 -Dversion=$DATA_GRID_VERSION \
 -DpomFile=${XAP_HOME}/tools/maven/poms/gs-runtime/pom.xml \
 -Dpackaging=jar \
 -Dfile=${XAP_HOME}/lib/required/gs-runtime.jar

mvn install:install-file \
 -DgroupId=com.gigaspaces \
 -DcreateChecksum=true \
 -DartifactId=gs-openspaces \
 -Dversion=$DATA_GRID_VERSION \
 -DpomFile=${XAP_HOME}/tools/maven/poms/gs-openspaces/pom.xml \
 -Dpackaging=jar \
 -Dfile=${XAP_HOME}/lib/required/gs-openspaces.jar

echo "Installing InsightEdge $INSIGHTEDGE_VER artifacts"

mvn install:install-file \
    -Dpackaging=pom \
    -Dfile=${INSIGHTEDGE_HOME}/tools/maven/poms/insightedge-package/pom.xml \
    -DpomFile=${INSIGHTEDGE_HOME}/tools/maven/poms/insightedge-package/pom.xml

mvn install:install-file \
 -DgroupId=com.gigaspaces.insightedge \
 -DcreateChecksum=true \
 -DartifactId=insightedge-core \
 -Dversion=$INSIGHTEDGE_VER \
 -DpomFile=${INSIGHTEDGE_HOME}/tools/maven/poms/insightedge-core/pom.xml \
 -Dpackaging=jar \
 -Dfile=${INSIGHTEDGE_HOME}/lib/insightedge-core-${INSIGHTEDGE_VER}.jar

mvn install:install-file \
 -DgroupId=com.gigaspaces.insightedge \
 -DcreateChecksum=true \
 -DartifactId=gigaspaces-scala \
 -Dversion=$INSIGHTEDGE_VER \
 -DpomFile=${INSIGHTEDGE_HOME}/tools/maven/poms/gigaspaces-scala/pom.xml \
 -Dpackaging=jar \
 -Dfile=${INSIGHTEDGE_HOME}/lib/gigaspaces-scala-${INSIGHTEDGE_VER}.jar


