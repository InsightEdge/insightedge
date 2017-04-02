#!/bin/bash

# This scripts installs InsightEdge artifacts(jars) to local maven repository

INSIGHTEDGE_VER=1.0.1

if [ -z "${INSIGHTEDGE_HOME}" ]; then
  INSIGHTEDGE_HOME="$(cd "`dirname "$0"`"/..; pwd)"
fi

echo "Installing InsightEdge $INSIGHTEDGE_VER artifacts"

mvn install:install-file \
    -Dpackaging=pom \
    -Dfile=${INSIGHTEDGE_HOME}/tools/maven/poms/insightedge-package/pom.xml \
    -DpomFile=${INSIGHTEDGE_HOME}/tools/maven/poms/insightedge-package/pom.xml

mvn install:install-file \
 -DgroupId=org.gigaspaces.insightedge \
 -DcreateChecksum=true \
 -DartifactId=insightedge-core \
 -Dversion=$INSIGHTEDGE_VER \
 -DpomFile=${INSIGHTEDGE_HOME}/tools/maven/poms/insightedge-core/pom.xml \
 -Dpackaging=jar \
 -Dfile=${INSIGHTEDGE_HOME}/lib/insightedge-core-${INSIGHTEDGE_VER}.jar

mvn install:install-file \
 -DgroupId=org.gigaspaces.insightedge \
 -DcreateChecksum=true \
 -DartifactId=insightedge-scala \
 -Dversion=$INSIGHTEDGE_VER \
 -DpomFile=${INSIGHTEDGE_HOME}/tools/maven/poms/insightedge-scala/pom.xml \
 -Dpackaging=jar \
 -Dfile=${INSIGHTEDGE_HOME}/lib/insightedge-scala-${INSIGHTEDGE_VER}.jar

# Install spring.aopalliance to local maven repo (fixes SBT builds)
mvn dependency:get \
 -Dartifact=org.aopalliance:com.springsource.org.aopalliance:1.0.0 \
 -DremoteRepositories=http://repository.springsource.com/maven/bundles/external/
