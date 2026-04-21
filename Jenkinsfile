pipeline {
    agent any

    environment {
        K8S_NAMESPACE   = 'bank-dev'
        HELM_RELEASE    = 'bank'
        HELM_CHART_PATH = './helm/bank'
        IMAGE_TAG       = "${env.BUILD_NUMBER}"
        SERVICES        = 'accounts-service cash-service transfer-service notifications-service gateway-service bank-front-ui'
    }

    options {
        buildDiscarder(logRotator(numToKeepStr: '5'))
        timeout(time: 30, unit: 'MINUTES')
        disableConcurrentBuilds()
        timestamps()
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build') {
            steps {
                sh 'mvn clean package -DskipTests --batch-mode --no-transfer-progress'
            }
            post {
                success {
                    archiveArtifacts artifacts: '**/target/*.jar', fingerprint: true
                }
            }
        }

        stage('Tests') {
            steps {
                sh 'mvn verify --batch-mode --no-transfer-progress'
            }
            post {
                always {
                    junit testResults: '**/target/*-reports/*.xml', allowEmptyResults: true
                }
            }
        }

        stage('Docker Build') {
            steps {
                sh '''
                    eval $(minikube docker-env)
                    for SERVICE in ${SERVICES}; do
                        docker build -t "${SERVICE}:${IMAGE_TAG}" -t "${SERVICE}:latest" "./${SERVICE}"
                    done
                '''
            }
        }

        stage('Helm Prepare') {
            steps {
                sh '''
                    helm dependency update ${HELM_CHART_PATH}
                    helm lint ${HELM_CHART_PATH} --set global.imageTag=${IMAGE_TAG}
                '''
            }
        }

        stage('Deploy') {
            steps {
                sh '''
                    kubectl get namespace ${K8S_NAMESPACE} || kubectl create namespace ${K8S_NAMESPACE}

                    helm upgrade --install ${HELM_RELEASE} ${HELM_CHART_PATH} \
                        --namespace ${K8S_NAMESPACE} \
                        --create-namespace \
                        --set global.imageTag=${IMAGE_TAG} \
                        --set global.imagePullPolicy=Never \
                        --wait --atomic --timeout 5m
                '''
            }
        }

        stage('Verify & Test') {
            steps {
                sh '''
                    kubectl get pods -n ${K8S_NAMESPACE}
                    helm test ${HELM_RELEASE} --namespace ${K8S_NAMESPACE} --timeout 3m --logs
                '''
            }
        }
    }

    post {
        failure {
            echo "Deployment failed on stage: ${env.STAGE_NAME}"
        }
        always {
            cleanWs()
        }
    }
}