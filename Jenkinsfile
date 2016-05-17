def String getBranchOrDefault(String repo, String targetBranch, String defaultBranch) {
    // write a number of branches matching target to a file
    // never fails with non-zero status code (using ||: syntax)
    sh "git ls-remote --heads $repo | grep -c $targetBranch > temp-branch-count || :"
    String branchMatchCount = readFile("temp-branch-count").trim()
    if (branchMatchCount == "1") {
        echo "Branch $targetBranch found at: $repo"
        return targetBranch
    } else {
        echo "Found $branchMatchCount branches matching $targetBranch at: $repo; Falling to default branch: $defaultBranch"
        return defaultBranch
    }
}

node {
    String branchName = "$env.BRANCH_NAME"

    String zeppelinRepo = "https://\$USERNAME:\$PASSWORD@github.com/InsightEdge/insightedge-zeppelin.git"
    String zeppelinDefaultBranchName = "branch-0.5.6"

    String examplesRepo = "https://\$USERNAME:\$PASSWORD@github.com/InsightEdge/insightedge-examples.git"
    String examplesDefaultBranchName = "master"

    echo "Branch: $branchName"


    withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: 'insightedge-dev', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD']]) {

        stage 'Checkout insightedge'
        checkout scm


        stage 'Build insightedge'
        env.PATH = "${tool 'maven-3.3.9'}/bin:$env.PATH"
        sh "mvn clean install -Dcom.gs.home=$env.XAP_HOME_DIR"


        stage 'Checkout zeppelin'
        String zeppelinBranchName = getBranchOrDefault(zeppelinRepo, branchName, zeppelinDefaultBranchName)
        echo "Using $zeppelinBranchName branch for Zeppelin"
        sh "rm -r zeppelin"
        sh "git clone -b $zeppelinBranchName --single-branch $zeppelinRepo zeppelin/$zeppelinBranchName"


        stage 'Build zeppelin'
        // sh "mvn -f zeppelin/$zeppelinBranchName clean install -DskipTests -P spark-1.6 -P build-distr"


        stage 'Checkout examples'
        String examplesBranchName = getBranchOrDefault(examplesRepo, branchName, examplesDefaultBranchName)
        echo "Using $examplesBranchName branch for examples"
        sh "rm -r examples"
        sh "git clone -b $examplesBranchName --single-branch $examplesRepo examples/$examplesBranchName"


        stage 'Build examples'
        sh "mvn -f examples/$examplesBranchName clean test package"

        
        stage 'Package insightedge'
        distributions = "-Ddist.spark=$env.SPARK_DIST"
        distributions = "$distributions -Ddist.xap=$env.XAP_DIST"
        distributions = "$distributions -Ddist.zeppelin=zeppelin/$zeppelinBranchName/zeppelin-distribution/target/zeppelin-0.5.7-incubating-SNAPSHOT.tar.gz"
        distributions = "$distributions -Ddist.examples=examples/$examplesBranchName/target/insightedge-examples.jar"
        sh "mvn package -pl insightedge-packager -DskipTests=true -P package-deployment $distributions -Dlast.commit.hash=$env.GIT_COMMIT"
        
    }
}