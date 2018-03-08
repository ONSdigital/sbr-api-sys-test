#!groovy
@Library('jenkins-pipeline-shared@develop') _

pipeline {
    environment {

        BRANCH_DEV = "develop"
        BRANCH_TEST = "release"
        BRANCH_PROD = "master"

        DEPLOY_DEV = "dev"
        DEPLOY_TEST = "test"
        DEPLOY_PROD = "prod"

        GIT_TYPE = "Github"
        GIT_CREDS = "github-sbr-user"
        GITLAB_CREDS = "sbr-gitlab-id"

        ORGANIZATION = "ons"
        TEAM = "sbr"
        MODULE_NAME = "sbr-api-sys-test"

        STAGE = "NONE"
    }
    options {
        skipDefaultCheckout()
        buildDiscarder(logRotator(numToKeepStr: '30', artifactNumToKeepStr: '30'))
        timeout(time: 30, unit: 'MINUTES')
        timestamps()
    }
    agent any
    parameters{
        choice(
            choices: 'Cloud Foundry\nGateway',
            description: 'Choose a system migration test suite option - either for Cloud Foundry or gateway. Note testing the gateway also tests the respective CF app.',
            name: 'SYS_TEST_TYPE'
        )
    }
    stages {
        stage('Checkout'){
            agent any
            steps{
                deleteDir()
                checkout scm
                stash name: 'app'
                sh "$SBT version"
                script {
                    version = '1.0.' + env.BUILD_NUMBER
                    currentBuild.displayName = version
                    STAGE = "Checkout"
                }
            }
        }
        stage ('Build') {
            agent any
            steps {
                script {
                    colourText("info", "Running system tests for ${SYS_TEST_TYPE}")
                    sh "$SBT clean compile"
                }
            }
        }
        // bundle all libs and dependencies
        stage ('Bundle') {
            agent any
            when {
                anyOf {
                    branch BRANCH_DEV
                    branch BRANCH_TEST
                    branch BRANCH_PROD
                }
            }
            steps {
                script {
                    STAGE = "Bundle"
                    if (BRANCH_NAME == BRANCH_DEV) {
                        env.DEPLOY_NAME = DEPLOY_DEV
                    }
                    else if  (BRANCH_NAME == BRANCH_TEST) {
                        env.DEPLOY_NAME = DEPLOY_TEST
                    }
                    else if (BRANCH_NAME == BRANCH_PROD) {
                        env.DEPLOY_NAME = DEPLOY_PROD
                    }
                    else {
                        colourText("info", "No matching branch")
                    }
                }
                dir('conf') {
                    git(url: "$GITLAB_URL/StatBusReg/${MODULE_NAME}.git", credentialsId: GITLAB_CREDS, branch: "${BRANCH_DEV}")
                }
                def sysTestDirectory = params.SYS_TEST_TYPE.toLowerCase().replaceAll("\\s","")
                colourText("info", "Bundling.... adding application.conf from ${sysTestDirectory} directory")
                sh "cp conf/${env.DEPLOY_NAME}/${sysTestDirectory}/application.conf src/test/resources"
            }
        }
        stage('Testing'){
            agent any
            when {
                anyOf {
                    branch BRANCH_DEV
                    branch BRANCH_TEST
                    branch BRANCH_PROD
                }
            }
            steps {
                colourText("info", "Building ${env.BUILD_ID} on ${env.JENKINS_URL} from branch ${env.BRANCH_NAME}")
                script {
                    colourText("info", "Bundling.... adding application.conf")
                    sh "$SBT clean compile test"
//                    stash name: 'compiled'
                }
            }
            post {
                always {
                    script {
                        STAGE = "Testing"
                    }
                }
                success {
                    colourText("info","Passed all system test for all SBR apis.")
                }
                failure {
                    colourText("warn","Failed in system tests for SBR apis.")
                }
            }
        }

    }
    post {
        always {
            script {
                colourText("info", 'Post steps initiated')
                deleteDir()
            }
        }
        success {
            colourText("success", "All stages complete. Build was successful.")
            sendNotifications currentBuild.result, "\$SBR_EMAIL_LIST"
        }
        unstable {
            colourText("warn", "Something went wrong, build finished with result ${currentResult}. This may be caused by failed tests, code violation or in some cases unexpected interrupt.")
            sendNotifications currentBuild.result, "\$SBR_EMAIL_LIST", "${STAGE}"
        }
        failure {
            colourText("warn","Process failed at: ${STAGE}")
            sendNotifications currentBuild.result, "\$SBR_EMAIL_LIST", "${STAGE}"
        }
    }
}


