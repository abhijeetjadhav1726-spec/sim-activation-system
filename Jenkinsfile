pipeline {
    agent any

    parameters {
        booleanParam(name: 'PUBLISH_IMAGE', defaultValue: false, description: 'Push the versioned image to Docker Hub (requires dockerhub credentials)')
        string(name: 'DOCKER_IMAGE', defaultValue: 'your-dockerhub-user/sim-activation', description: 'Registry image name')
        string(name: 'DEPLOY_HOST', defaultValue: '', description: 'Optional SSH host for deployment')
        string(name: 'DEPLOY_USER', defaultValue: '', description: 'SSH deployment user')
    }

    options {
        timestamps()
        disableConcurrentBuilds()
    }

    environment {
        APP_NAME = 'sim-activation'
        IMAGE_TAG = "${BUILD_NUMBER}"
        APP_PORT = '8080'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }
        stage('Build and unit test') {
            steps {
                sh 'mvn -B clean test'
            }
            post {
                always {
                    junit 'target/surefire-reports/*.xml'
                }
            }
        }
        stage('Package') {
            steps {
                sh 'mvn -B -DskipTests package'
                archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
            }
        }
        stage('Selenium journeys') {
            when {
                branch 'main'
            }
            steps {
                sh 'mvn -B -Pselenium verify'
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: 'target/failsafe-reports/*.xml'
                    archiveArtifacts allowEmptyArchive: true, artifacts: 'target/selenium-failures/*.png'
                }
            }
        }
        stage('Build container image') {
            when {
                branch 'main'
            }
            steps {
                sh 'docker build --pull -t ${APP_NAME}:${IMAGE_TAG} -t ${APP_NAME}:latest .'
            }
        }
        stage('Publish image') {
            when {
                allOf {
                    branch 'main'
                    expression { return params.PUBLISH_IMAGE }
                }
            }
            steps {
                withCredentials([usernamePassword(credentialsId: 'dockerhub', usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASSWORD')]) {
                    sh 'echo "$DOCKER_PASSWORD" | docker login --username "$DOCKER_USER" --password-stdin'
                    sh 'docker tag ${APP_NAME}:${IMAGE_TAG} "${DOCKER_IMAGE}:${IMAGE_TAG}"'
                    sh 'docker push "${DOCKER_IMAGE}:${IMAGE_TAG}"'
                    sh 'docker logout'
                }
            }
        }
        stage('Deploy') {
            when {
                allOf {
                    branch 'main'
                    expression { return params.PUBLISH_IMAGE && params.DEPLOY_HOST?.trim() }
                }
            }
            steps {
                withCredentials([sshUserPrivateKey(credentialsId: 'sim-deploy-key', keyFileVariable: 'SSH_KEY')]) {
                    sh 'ssh -i "$SSH_KEY" -o BatchMode=yes "$DEPLOY_USER@$DEPLOY_HOST" "docker pull $DOCKER_IMAGE:$IMAGE_TAG && (docker rm -f sim-activation || true) && docker run -d --restart unless-stopped --name sim-activation -p $APP_PORT:8080 -v sim-activation-data:/app/data $DOCKER_IMAGE:$IMAGE_TAG"'
                }
            }
        }
    }
}
