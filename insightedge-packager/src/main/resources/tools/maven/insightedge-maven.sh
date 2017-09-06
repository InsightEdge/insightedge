#!/bin/bash
set -e
# This scripts installs InsightEdge artifacts (jars) to local maven repository

DIRNAME=$(dirname ${BASH_SOURCE[0]})
source ${DIRNAME}/../../conf/insightedge-env.sh

echo "Installing InsightEdge artifacts"

mvn install:install-file \
    -Dpackaging=pom \
    -Dfile=${XAP_HOME}/insightedge/tools/maven/poms/insightedge-package/pom.xml \
    -DpomFile=${XAP_HOME}/insightedge/tools/maven/poms/insightedge-package/pom.xml

mvn install:install-file \
 -DgroupId=org.gigaspaces.insightedge \
 -DcreateChecksum=true \
 -DartifactId=insightedge-core \
 -DpomFile=${XAP_HOME}/insightedge/tools/maven/poms/insightedge-core/pom.xml \
 -Dpackaging=jar \
 -Dfile=${XAP_HOME}/insightedge/lib/insightedge-core.jar

# Install spring.aopalliance to local maven repo (fixes SBT builds)
mvn dependency:get \
 -Dartifact=org.aopalliance:com.springsource.org.aopalliance:1.0.0 \
 -DremoteRepositories=http://repository.springsource.com/maven/bundles/external/
