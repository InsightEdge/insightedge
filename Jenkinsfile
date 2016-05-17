node {
    withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: 'insightedge-dev', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD']]) {

        branchName = "${env.BRANCH_NAME}"
        zeppelinDefaultBranchName = "branch-0.5.6"
        zeppelinRepo = "https://\$USERNAME:\$PASSWORD@github.com/InsightEdge/insightedge-zeppelin.git"
        zeppelinBranchFile = "zeppelin-branch-count"
        echo "Branch: ${branchName}"

        stage 'Checkout insightedge'
        checkout scm


        stage 'Build insightedge'
        env.PATH = "${tool 'maven-3.3.9'}/bin:${env.PATH}"
        // sh "mvn clean install -Dcom.gs.home=${env.XAP_HOME_DIR}"


        stage 'Checkout zeppelin'
        // write a number of branches matching current BRANCH_NAME to a file 'zeppelinBranchFile'
        // never fails with non-zero status code (using ||: syntax)
        sh "git ls-remote --heads ${zeppelinRepo} | grep -c ${branchName} > ${zeppelinBranchFile} || :"
        branchMatchCount = readFile(zeppelinBranchFile).trim()
        if (branchMatchCount == "1") {
            echo "Branch ${branchName} found in Zeppelin at: ${zeppelinRepo}"
            echo "Using ${branchName} for Zeppelin"
            zeppelinBranchName = "${branchName}"
        } else {
            echo "Found ${branchMatchCount} branches matching ${branchName} at: ${zeppelinRepo}"
            echo "Using default branch for Zeppelin: ${zeppelinDefaultBranchName}"
            zeppelinBranchName = "${zeppelinDefaultBranchName}"
        }

        // checkout Zeppelin repo
        sh "git clone -b ${zeppelinBranchName} --single-branch ${zeppelinRepo} zeppelin"


        stage 'Build zeppelin'
        sh "mvn -f zeppelin/pom.xml clean install -DskipTests -P spark-1.6 -P build-distr"

    }
}