#!/bin/bash
set -e
# This scripts installs InsightEdge artifacts(jars) to local maven repository

if [ -z "${XAP_HOME}" ]; then
  export XAP_HOME="$(cd $(dirname ${BASH_SOURCE[0]})/../..; pwd)"
fi
export IE_PATH_INTERNAL="${XAP_HOME}/insightedge"

echo "Installing InsightEdge artifacts"

mvn install:install-file \
    -Dpackaging=pom \
    -Dfile=${IE_PATH_INTERNAL}/tools/maven/poms/insightedge-package/pom.xml \
    -DpomFile=${IE_PATH_INTERNAL}/tools/maven/poms/insightedge-package/pom.xml

mvn install:install-file \
 -DgroupId=org.gigaspaces.insightedge \
 -DcreateChecksum=true \
 -DartifactId=insightedge-core \
 -DpomFile=${IE_PATH_INTERNAL}/tools/maven/poms/insightedge-core/pom.xml \
 -Dpackaging=jar \
 -Dfile=${IE_PATH_INTERNAL}/lib/insightedge-core.jar

# Install spring.aopalliance to local maven repo (fixes SBT builds)
mvn dependency:get \
 -Dartifact=org.aopalliance:com.springsource.org.aopalliance:1.0.0 \
 -DremoteRepositories=http://repository.springsource.com/maven/bundles/external/
