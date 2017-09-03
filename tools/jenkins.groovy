def String getBranchOrDefault(String repo, String targetBranch, String defaultBranch) {
    // default branch is preferred over master
    if (targetBranch.equals("master")) {
        return defaultBranch
    }

    // write a number of branches matching target to a file
    sh "git ls-remote --heads $repo | grep -c $targetBranch > temp-branch-count"
    String branchMatchCount = readFile("temp-branch-count").trim()
    if (branchMatchCount.equals("1")) {
        echo "Branch $targetBranch found at: $repo"
        return targetBranch
    } else {
        echo "Found $branchMatchCount branches matching $targetBranch at: $repo; Falling to default branch: $defaultBranch"
        return defaultBranch
    }
}

def String calcBuildNumber() {
    return INSIGHTEDGE_BUILD_NUMBER + "-" + "$env.BUILD_NUMBER".toString()
}

def String calcBranch() {
    sh 'git branch --remote --verbose --no-abbrev --contains | sed -rne \'s/^[^\\/]*\\/([^\\ ]+).*$/\\1/p\' > temp-git-branch'
    return readFile("temp-git-branch").trim()
}

String branchName = calcBranch()
String xapOpenUrl = XAP_OPEN_URL
String xapPremiumUrl = XAP_PREMIUM_URL
String buildNumber = calcBuildNumber()
boolean shouldPublishPrivateArtifacts = PUBLISH_NEWMAN_ARTIFACTS.toBoolean()
String newmanTags = NEWMAN_TAGS

echo "XAP open URL: " + xapOpenUrl
echo "XAP premium URL: " + xapPremiumUrl
echo "InsightEdge build number: " + buildNumber
echo "Branch: $branchName"
echo "Publish newman artifacts: " + shouldPublishPrivateArtifacts
echo "Newman tags: " + newmanTags

String zeppelinRepo = "git@github.com:InsightEdge/insightedge-zeppelin.git"
String zeppelinDefaultBranchName = "master"

String examplesRepo = "git@github.com:InsightEdge/insightedge-examples.git"
String examplesDefaultBranchName = "master"

sh 'git log -1 --format="%H" > temp-git-commit-hash'
String commitHash = readFile("temp-git-commit-hash").trim()
echo "Commit: $commitHash"


stage 'Build insightedge'
try {
    load 'tools/build.groovy'
} finally {
    step([$class: 'JUnitResultArchiver', testResults: 'insightedge-core/target/surefire-reports/TEST-*.xml'])
}


stage 'Checkout zeppelin'
String zeppelinBranchName = getBranchOrDefault(zeppelinRepo, branchName, zeppelinDefaultBranchName)
echo "Using $zeppelinBranchName branch for Zeppelin"
sh "rm -r zeppelin || :"
sh "git clone -b $zeppelinBranchName --single-branch $zeppelinRepo zeppelin/$zeppelinBranchName"


stage 'Build zeppelin'
dir("zeppelin/$zeppelinBranchName") {
    load 'tools/build.groovy'
}

stage 'Checkout examples'
String examplesBranchName = getBranchOrDefault(examplesRepo, branchName, examplesDefaultBranchName)
echo "Using $examplesBranchName branch for examples"
sh "rm -r examples || :"
sh "git clone -b $examplesBranchName --single-branch $examplesRepo examples/$examplesBranchName"


stage 'Build examples'
dir("examples/$examplesBranchName") {
    load 'tools/build.groovy'
}


stage 'Package insightedge'
distributions = "-Ddist.spark=$env.SPARK_DIST"
distributions = "$distributions -Ddist.zeppelin=zeppelin/$zeppelinBranchName/zeppelin-distribution/target/zeppelin-0.6.1-SNAPSHOT.tar.gz"
distributions = "$distributions -Ddist.examples.target=examples/$examplesBranchName/target"
premiumDist = "$distributions -Ddist.xap=" + xapPremiumUrl
communityDist = "$distributions -Ddist.xap=" + xapOpenUrl
sh "mvn package -pl insightedge-packager -P package-premium   -DskipTests=true $premiumDist   -Dlast.commit.hash=$commitHash"
sh "mvn package -pl insightedge-packager -P package-community -DskipTests=true $communityDist -Dlast.commit.hash=$commitHash"


stage 'Export artifacts'
archive 'insightedge-packager/target/community/gigaspaces-*.zip'
archive 'insightedge-packager/target/premium/gigaspaces-*.zip'

if (shouldPublishPrivateArtifacts) {
    stage 'Publish private artifacts to remote disk'
    String publishSystemProps = "-Dinsightedge.branch=" + branchName + " -Dinsightedge.build.number=" + buildNumber + " -Dnewman.tags=" + newmanTags
    sh "mvn package -pl insightedge-packager -P publish-artifacts  -DskipTests=true " + publishSystemProps
}
else {
    stage 'Synchronize integration tests'
    String lockMessage = "branch=$branchName;commit=$commitHash"
    sh "chmod +x tools/lock.sh"
    sh "chmod +x tools/unlock.sh"
    // lock with 15 minutes timeout - expected time for integration tests to be finished
    sh "tools/lock.sh /tmp/integration-tests.lock 900 30 \"$lockMessage\""

    try {
        try {
            stage 'Run integration tests (community)'
            sh "mvn clean verify -pl insightedge-integration-tests -P run-integration-tests-community -e"
        } finally {
            step([$class: 'JUnitResultArchiver', testResults: 'insightedge-integration-tests/target/surefire-reports/TEST-*.xml'])
        }

        try {
            stage 'Run integration tests (premium)'
            sh "mvn clean verify -pl insightedge-integration-tests -P run-integration-tests-premium -e"
        } finally {
            step([$class: 'JUnitResultArchiver', testResults: 'insightedge-integration-tests/target/surefire-reports/TEST-*.xml'])
        }

        if (branchName.equals("master") || branchName.startsWith("branch-")) {
            try {
                stage 'Run long integration tests (community)'
                sh "mvn clean verify -pl insightedge-integration-tests -P run-integration-tests-community,only-long-running-test -e -Dgit.branch=$branchName"
            } finally {
                step([$class: 'JUnitResultArchiver', testResults: 'insightedge-integration-tests/target/surefire-reports/TEST-*.xml'])
            }

            try {
                stage 'Run long integration tests (premium)'
                sh "mvn clean verify -pl insightedge-integration-tests -P run-integration-tests-premium,only-long-running-test -e -Dgit.branch=$branchName"
            } finally {
                step([$class: 'JUnitResultArchiver', testResults: 'insightedge-integration-tests/target/surefire-reports/TEST-*.xml'])
            }
        } else {
            echo 'Skip long running integration tests'
        }

    } finally {
        // if tests fail - unlock the lock anyway
        sh "tools/unlock.sh /tmp/integration-tests.lock \"$lockMessage\""
    }
}
