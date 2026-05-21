pipeline {
    agent any

    environment {
        VAADIN_USAGE_STATS_ENABLED = 'false'
        PLATEMATE_RUNTIME_PATH = 'C:\\apps\\platemate\\current'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Test') {
            steps {
                bat 'mvn test'
            }
        }

        stage('Deploy') {
            steps {
                bat 'powershell -NoProfile -ExecutionPolicy Bypass -File deployment\\deploy.ps1 -SkipGitPull'
            }
        }

        stage('Smoke Check') {
            steps {
                bat 'powershell -NoProfile -ExecutionPolicy Bypass -File deployment\\smoke-check.ps1'
            }
        }
    }

    post {
        success {
            echo 'PlateMate pipeline completed successfully.'
        }
        failure {
            echo 'PlateMate pipeline failed. Check the console output above.'
        }
    }
}
