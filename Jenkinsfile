#!groovy
@Library('jenkins-pipeline-shared@feature/version') _

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
    }
    options {
        skipDefaultCheckout()
        buildDiscarder(logRotator(numToKeepStr: '30', artifactNumToKeepStr: '30'))
        timeout(time: 30, unit: 'MINUTES')
        timestamps()
    }
    agent any
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
                    env.NODE_STAGE = "Checkout"
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
                    env.NODE_STAGE = "Bundle"
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
                colourText("info", "Bundling.... adding application.conf")
                sh "cp conf/${env.DEPLOY_NAME}/application.conf src/test/resources"
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
                        env.NODE_STAGE = "Testing"
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
            sendNotifications currentBuild.result, "\$SBR_EMAIL_LIST", "${env.NODE_STAGE}"
        }
        failure {
            colourText("warn","Process failed at: ${env.NODE_STAGE}")
            sendNotifications currentBuild.result, "\$SBR_EMAIL_LIST", "${env.NODE_STAGE}"
        }
    }
}


