node {
    echo "Branch: ${env.BRANCH_NAME}"

    sh "git ls-remote --heads"
    sh "git ls-remote --heads ${env.ZEPPELIN_REPO}"

    stage 'Checkout insightedge'
    checkout scm


    stage 'Build insightedge'
    env.PATH = "${tool 'maven-3.3.9'}/bin:${env.PATH}"
    // sh "mvn clean install -Dcom.gs.home=${env.XAP_HOME_DIR}"


    stage 'Running build-zeppelin.sh'
    sh "chmod +x ./build-zeppelin.sh"
    sh "./build-zeppelin.sh"
}