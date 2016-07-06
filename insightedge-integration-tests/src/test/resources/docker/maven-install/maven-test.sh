#!/usr/bin/env bash

#TODO rename file

function add_insightedge_libs_to_repo() {
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

println "------ Testing Insighetde maven installation script"
echo "-- Logged as "`whoami`
echo "-- Maven version "
mvn --version
echo "-- SBT version "
sbt sbtVersion

HOME="/home/ie-user"
EXPECTED_PROJECTS_COUNT=10

cd $HOME
git clone https://github.com/InsightEdge/insightedge-examples.git
cd $HOME/insightedge-examples

println "------ Maven build should fail"
rm -rf $HOME/.m2
mvn clean test package | tee $HOME/insightedge-examples-mvn-fail.out
maven_failed=`grep "\[INFO\] BUILD FAILURE" $HOME/insightedge-examples-mvn-fail.out | wc -l`
if [[ $maven_failed == 1 ]]; then
    println "------ OK: Maven build failed"
else
    println "------ ERROR: Maven build should have failed"
    exit 1
fi

println "------ Maven build should succeed"
rm -rf $HOME/.m2
add_insightedge_libs_to_repo
mvn clean test package | tee $HOME/insightedge-examples-mvn-success.out
maven_succeed=`grep "\[INFO\] BUILD SUCCESS" $HOME/insightedge-examples-mvn-success.out | wc -l`
if [[ $maven_succeed == 1 ]]; then
    println "------ OK: Maven build succeeded"
else
    println "------ ERROR: Maven build failed"
    exit 1
fi

println "------ SBT build should fail"
rm -rf $HOME/.m2
sbt clean test assembly | tee $HOME/insightedge-examples-sbt-fail.out
sbt_failed=`grep "\[error\]" $HOME/insightedge-examples-sbt-fail.out | wc -l`
if [[ $sbt_failed < 1 ]]; then
    println "---- OK: SBT build failed"
else
    println "---- ERROR: SBT build should have failed"
    exit 1
fi

println "------ SBT build should succeed"
rm -rf $HOME/.m2
add_insightedge_libs_to_repo
sbt clean test assembly | tee $HOME/insightedge-examples-sbt-success.out
sbt_succeed=`grep "[success]" $HOME/insightedge-examples-sbt-success.out | wc -l`
if [[ $sbt_succeed < 1 ]]; then
    println "---- OK: SBT build succeeded"
else
    println "---- ERROR SBT build failed"
    exit 1
fi

exit 0