#!/usr/bin/env bash

function add_insightedge_libs_to_repo() {
    EXPECTED_PROJECTS_COUNT=11
    /opt/gigaspaces-insightedge/sbin/insightedge-maven.sh | tee $HOME/insightedge-maven.out
    projects_count=`grep "BUILD SUCCESS" $HOME/insightedge-maven.out | wc -l`
    if [[ $projects_count == $EXPECTED_PROJECTS_COUNT ]]; then
        echo "Was installed $projects_count projects"
    else
        echo "Wrong maven projects count. Expected $EXPECTED_PROJECTS_COUNT, result $projects_count"
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
echo "-- Maven version"
mvn --version
echo "-- SBT version "
sbt sbtVersion # it fails sometimes

HOME="/home/ie-user"

cd $HOME
git clone https://github.com/InsightEdge/insightedge-examples.git
cd $HOME/insightedge-examples
git checkout ie-100_check_build # TODO should be master

println "------ Maven build should fail"
rm -rf $HOME/.m2
mvn clean test package | tee $HOME/insightedge-examples-mvn-fail.out
maven_failed=`grep "\[INFO\] BUILD FAILURE" $HOME/insightedge-examples-mvn-fail.out | wc -l`
if [[ $maven_failed > 0 ]]; then
    println "------ OK: Maven build failed"
else
    println "------ ERROR: Maven build should have failed"
    exit 1
fi

println "------ Maven build should succeed"
rm -rf $HOME/.m2
add_insightedge_libs_to_repo
mvn clean test package | tee $HOME/insightedge-examples-mvn-success.out
maven_failed=`grep "\[INFO\] BUILD FAILURE" $HOME/insightedge-examples-mvn-success.out | wc -l`
if [[ $maven_failed == 0 ]]; then
    println "------ OK: Maven build succeeded"
else
    println "------ ERROR: Maven build failed"
    exit 1
fi

println "------ SBT build should fail"
rm -rf $HOME/.m2
sbt clean test assembly -no-colors | tee $HOME/insightedge-examples-sbt-fail.out
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
sbt clean test assembly -no-colors | tee $HOME/insightedge-examples-sbt-success.out
sbt_failed=`grep -c "\[error\]" $HOME/insightedge-examples-sbt-success.out`
if [[ $sbt_failed == 0 ]]; then
    println "---- OK: SBT build succeeded"
else
    println "---- ERROR SBT build failed"
    exit 1
fi

exit 0