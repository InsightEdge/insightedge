#!/bin/bash

# This scripts installs InsightEdge artifacts(jars) to local maven repository

INSIGHTEDGE_VER=0.4.0-SNAPSHOT

if [ -z "${INSIGHTEDGE_HOME}" ]; then
  INSIGHTEDGE_HOME="$(cd "`dirname "$0"`"/..; pwd)"
fi

#XAP_HOME=${INSIGHTEDGE_HOME}/datagrid
#echo "Data Grid home $XAP_HOME"

#if [ -z "${JAVA_HOME}" ]; then
#	export JAVACMD=java
#else
#	export JAVACMD="${JAVA_HOME}/bin/java"
#fi


#POMS_DIR="${XAP_HOME}/tools/maven/poms"
#XAP_DATAGRID_POMS_DIR="${POMS_DIR}/xap-datagrid"
#XAP_DATAGRID_CORE_POMS="${XAP_DATAGRID_POMS_DIR}/xap-core"
#XAP_DATAGRID_EXT_POMS="${XAP_DATAGRID_POMS_DIR}/xap-extensions"


#echo "Installing Data Grid $DATA_GRID_VERSION artifacts"

# GigaSpaces Jars
#mvn install:install-file -DcreateChecksum=true -DpomFile=${XAP_DATAGRID_POMS_DIR}/pom.xml -Dfile=${XAP_DATAGRID_POMS_DIR}/pom.xml

# open core modules
#mvn install:install-file -DcreateChecksum=true -DpomFile=${XAP_DATAGRID_CORE_POMS}/xap-common/pom.xml -Dfile=${XAP_HOME}/lib/required/xap-common.jar
#mvn install:install-file -DcreateChecksum=true -DpomFile=${XAP_DATAGRID_CORE_POMS}/xap-trove/pom.xml -Dfile=${XAP_HOME}/lib/required/xap-trove.jar
#mvn install:install-file -DcreateChecksum=true -DpomFile=${XAP_DATAGRID_CORE_POMS}/xap-asm/pom.xml -Dfile=${XAP_HOME}/lib/required/xap-asm.jar
#mvn install:install-file -DcreateChecksum=true -DpomFile=${XAP_DATAGRID_CORE_POMS}/xap-datagrid/pom.xml -Dfile=${XAP_HOME}/lib/required/xap-datagrid.jar
#mvn install:install-file -DcreateChecksum=true -DpomFile=${XAP_DATAGRID_CORE_POMS}/xap-openspaces/pom.xml -Dfile=${XAP_HOME}/lib/required/xap-openspaces.jar -Dsources="${XAP_HOME}/lib/optional/openspaces/xap-openspaces-sources.jar"
#mvn install:install-file -DcreateChecksum=true -DpomFile=${XAP_DATAGRID_EXT_POMS}/xap-jms/pom.xml -Dfile=${XAP_HOME}/lib/optional/jms/xap-jms.jar
#mvn install:install-file -DcreateChecksum=true -DpomFile=${XAP_DATAGRID_EXT_POMS}/xap-spatial/pom.xml -Dfile=${XAP_HOME}/lib/optional/spatial/xap-spatial.jar


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


