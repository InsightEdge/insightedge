node {
    env.PATH = "${tool 'maven-3.3.9'}/bin:${env.PATH}"

    stage 'Build and Test'
    checkout scm
    sh "mvn clean install -Dcom.gs.home=${env.XAP_HOME_DIR}"
}