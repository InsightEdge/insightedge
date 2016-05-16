node {
    stage 'Checkout insightedge'
    checkout scm

    stage 'Build insightedge'
    env.PATH = "${tool 'maven-3.3.9'}/bin:${env.PATH}"
    sh "mvn clean install -Dcom.gs.home=${env.XAP_HOME_DIR}"

    stage 'Checkout zeppelin'
    env.ZEPPELIN_REPO = "https://github.com/InsightEdge/insightedge-zeppelin.git"
    echo "Branch: ${GIT_BRANCH}"
    if ( sh "git ls-remote --heads ${env.ZEPPELIN_REPO} | grep -q ${GIT_BRANCH}" ) {
        echo "Branch ${GIT_BRANCH} found in Zeppelin at: ${env.ZEPPELIN_REPO}"
    } else {
        echo "Branch ${GIT_BRANCH} not found in Zeppelin at: ${env.ZEPPELIN_REPO}"
        echo "Using default branch for Zeppelin"
    }
}