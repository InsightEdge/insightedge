#!/usr/bin/env bash
set -x

function add_insightedge_libs_to_repo() {
    println "------ Installing libraries"
    EXPECTED_PROJECTS_COUNT=4
    /opt/gigaspaces-insightedge/insightedge/sbin/insightedge-maven.sh | tee $HOME/insightedge-maven.out
    local failed_builds=`grep -c "\[INFO\] BUILD FAILURE" $HOME/insightedge-maven.out`
    if [[ $failed_builds == 0 ]]; then
        println "------ OK: All libraries installed successfully"
    else
        println "------ ERROR: Some libraries were not installed"
        exit 1
    fi

    local projects_count=`grep -c "\[INFO\] BUILD SUCCESS" $HOME/insightedge-maven.out`
    if [[ $projects_count == $EXPECTED_PROJECTS_COUNT ]]; then
        println "------ OK: Was installed $projects_count projects"
    else
        println "------ ERROR: Wrong maven projects count. Expected $EXPECTED_PROJECTS_COUNT, result $projects_count"
        exit 1
    fi
}

function println() {
    echo ""
    echo "$1"
    echo ""
}

println "------ Testing Insighetde maven libs installation script"
echo "-- Logged as "`whoami`
echo "-- Environment"
env
echo "-- Java version"
java -version


BRANCH=$1
echo "-- Git branch: $BRANCH"
MVN_OR_SBT=$2
echo "-- Build option: $MVN_OR_SBT"
IE_VERSION=$3
echo "-- IE Version: $IE_VERSION"

cd $HOME
git clone https://github.com/InsightEdge/insightedge-examples.git
cd $HOME/insightedge-examples
git fetch
git checkout $BRANCH

if [ "${MVN_OR_SBT}" = "mvn" ]; then
echo "-- Maven version"
mvn --version
println "------ Maven build should fail"
rm -rf $HOME/.m2
mvn clean test package -o -Die.version=$IE_VERSION  | tee $HOME/insightedge-examples-mvn-fail.out
maven_failed=`grep -c "\[INFO\] BUILD FAILURE" $HOME/insightedge-examples-mvn-fail.out`
if [[ $maven_failed > 0 ]]; then
    println "------ OK: Maven build failed"
else
    println "------ ERROR: Maven build should have failed"
    exit 1
fi

println "------ Maven build should succeed"
rm -rf $HOME/.m2
add_insightedge_libs_to_repo
mvn clean test package -o -Die.version=$IE_VERSION  | tee $HOME/insightedge-examples-mvn-success.out
maven_failed=`grep -c "\[INFO\] BUILD FAILURE" $HOME/insightedge-examples-mvn-success.out`
if [[ $maven_failed == 0 ]]; then
    println "------ OK: Maven build succeeded"
else
    println "------ ERROR: Maven build failed"
    exit 1
fi

elif [ "${MVN_OR_SBT}" = "sbt" ]; then
echo "-- SBT version "
sbt sbtVersion # TODO it fails sometimes
println "------ SBT build should fail"
rm -rf $HOME/.m2
sbt -DinsightEdgeVersion=$IE_VERSION clean test assembly -no-colors | tee $HOME/insightedge-examples-sbt-fail.out
sbt_failed=`grep -c "\[error\]" $HOME/insightedge-examples-sbt-fail.out`
if [[ $sbt_failed > 0 ]]; then
    println "---- OK: SBT build failed"
else
    println "---- ERROR: SBT build should have failed"
    exit 1
fi

println "------ SBT build should succeed"
rm -rf $HOME/.m2
add_insightedge_libs_to_repo
sbt -DinsightEdgeVersion=$IE_VERSION clean test assembly -no-colors | tee $HOME/insightedge-examples-sbt-success.out
sbt_failed=`grep -c "\[error\]" $HOME/insightedge-examples-sbt-success.out`
if [[ $sbt_failed == 0 ]]; then
    println "---- OK: SBT build succeeded"
else
    println "---- ERROR: SBT build failed"
    exit 1
fi

else
    println "---- ERROR: SBT/MVN failed due to wrong/missing parameter : $MVN_OR_SBT "
    exit 1
fi

exit 0