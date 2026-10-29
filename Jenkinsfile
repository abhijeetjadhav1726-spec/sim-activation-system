pipeline {
    agent any

    parameters {
        booleanParam(name: 'PUBLISH_IMAGE', defaultValue: false, description: 'Push the versioned image to Docker Hub (requires dockerhub credentials)')
        booleanParam(name: 'DEPLOY_LOCAL', defaultValue: true, description: 'Deploy the tested image to this Jenkins agent using Docker')
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
        APP_PORT = '18080'
        DOCKER_IMAGE = "${params.DOCKER_IMAGE}"
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }
        stage('Build and unit test') {
            steps {
                bat 'mvn -B clean test'
            }
            post {
                always {
                    junit 'target/surefire-reports/*.xml'
                }
            }
        }
        stage('Package') {
            steps {
                bat 'mvn -B -DskipTests package'
                archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
            }
        }
        stage('Selenium journeys') {
            steps {
                bat 'mvn -B -Pselenium verify'
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
                bat 'docker build --pull -t %APP_NAME%:%IMAGE_TAG% -t %APP_NAME%:latest .'
            }
        }
        stage('Deploy local container') {
            when {
                allOf {
                    branch 'main'
                    expression { return params.DEPLOY_LOCAL }
                }
            }
            steps {
                powershell '''
                    $containerName = 'sim-activation'
                    $existing = docker ps -aq --filter "name=^/$containerName$"
                    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
                    if ($existing) {
                        docker rm -f $containerName
                        if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
                    }

                    docker run -d --restart unless-stopped --name $containerName -p "$env:APP_PORT`:8080" -v sim-activation-data:/app/data "$env:APP_NAME`:$env:IMAGE_TAG"
                    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

                    $healthUrl = "http://127.0.0.1:$env:APP_PORT/actuator/health"
                    $healthy = $false
                    for ($attempt = 1; $attempt -le 30; $attempt++) {
                        $status = curl.exe --silent --show-error --output NUL --write-out "%{http_code}" --max-time 5 $healthUrl
                        if ($LASTEXITCODE -eq 0 -and $status -eq '200') {
                            $healthy = $true
                            break
                        }
                        Write-Host "Health check attempt $attempt did not return HTTP 200 (status: $status)"
                        Start-Sleep -Seconds 3
                    }

                    if (-not $healthy) {
                        docker logs --tail 100 $containerName
                        docker rm -f $containerName
                        exit 1
                    }
                    Write-Host "Deployed $env:APP_NAME`:$env:IMAGE_TAG at $healthUrl"
                '''
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
                    powershell '''
                        $env:DOCKER_PASSWORD | docker login --username $env:DOCKER_USER --password-stdin
                        if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
                        docker tag "$env:APP_NAME`:$env:IMAGE_TAG" "$env:DOCKER_IMAGE`:$env:IMAGE_TAG"
                        if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
                        docker push "$env:DOCKER_IMAGE`:$env:IMAGE_TAG"
                        if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
                        docker logout
                    '''
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
                    powershell '''
                        $remoteCommand = "docker pull $env:DOCKER_IMAGE`:$env:IMAGE_TAG && (docker rm -f sim-activation || true) && docker run -d --restart unless-stopped --name sim-activation -p $env:APP_PORT`:8080 -v sim-activation-data:/app/data $env:DOCKER_IMAGE`:$env:IMAGE_TAG"
                        & ssh.exe -i $env:SSH_KEY -o BatchMode=yes "$env:DEPLOY_USER@$env:DEPLOY_HOST" $remoteCommand
                        if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
                    '''
                }
            }
        }
    }
}
