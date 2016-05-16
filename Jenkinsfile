node {
    sh "printenv"
    sh "git symbolic-ref --short HEAD"


    stage 'Checkout insightedge'
    checkout scm


    stage 'Build insightedge'
    env.PATH = "${tool 'maven-3.3.9'}/bin:${env.PATH}"
    sh "mvn clean install -Dcom.gs.home=${env.XAP_HOME_DIR}"


    stage 'Checkout zeppelin'
    // write a number of branches matching current BRANCH_NAME to a file "zeppelin-branch-exists"
    // never fails with non-zero status code (using ||: syntax)
    sh "git ls-remote --heads ${env.ZEPPELIN_REPO} | grep -c ${GIT_BRANCH} > zeppelin-branch-exists || :"
    BRANCH_MATCH_COUNT = readFile('zeppelin-branch-exists').trim()
    if ( BRANCH_MATCH_COUNT == "1" ) {
        echo "Branch ${GIT_BRANCH} found in Zeppelin at: ${env.ZEPPELIN_REPO}"
        echo "Using ${GIT_BRANCH} for Zeppelin"
        ZEPPELIN_BRANCH_NAME = "${GIT_BRANCH}"
    } else {
        echo "Found ${BRANCH_MATCH_COUNT} branches matching ${GIT_BRANCH} at: ${env.ZEPPELIN_REPO}"
        echo "Using default branch for Zeppelin: ${ZEPPELIN_DEFAULT_BRANCH_NAME}"
        ZEPPELIN_BRANCH_NAME = "${ZEPPELIN_DEFAULT_BRANCH_NAME}"
    }

    // checkout Zeppelin repo
    checkout([
        $class: 'GitSCM',
        branches: [[name: "*/${ZEPPELIN_BRANCH_NAME}"]],
        doGenerateSubmoduleConfigurations: false,
        extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: 'zeppelin']],
        submoduleCfg: [],
        userRemoteConfigs: [[url: 'https://github.com/InsightEdge/insightedge-zeppelin.git']]
    ])


    stage 'Build zeppelin'
    sh "mvn -f zeppelin/pom.xml clean install -DskipTests -P spark-1.6 -P build-distr"
}