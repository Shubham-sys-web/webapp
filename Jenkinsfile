pipeline {
    agent any
    stages {
        stage('Initialize') {
            steps {
                sh '''
                    echo "PATH = ${PATH}"
                    echo "M2_HOME = ${M2_HOME}"
                '''
            }
        }
        stage('Secret Scan - TruffleHog') {
            steps {
                script {
                    sh 'trufflehog github --repo=https://github.com/Shubham-sys-web/webapp.git --json --no-update > trufflehog-report.json || true'
                    sh 'cat trufflehog-report.json'

                    def report = readFile('trufflehog-report.json')
                    if (report.contains('"DetectorName"')) {
                        echo "🚨 SECRETS DETECTED by TruffleHog! Build aborted."
                        error "Pipeline stopped: hardcoded secrets found in repository."
                    } else {
                        echo "✅ No secrets detected. Proceeding to build."
                    }
                }
            }
        }
        stage('Build') {
            steps {
                sh 'mvn clean package'
            }
        }
        stage('Deploy To Tomcat') {
            steps {
                sshagent(['tomcat']) {
                    sh 'scp -o StrictHostKeyChecking=no target/*.war ubuntu@65.2.73.79:/opt/tomcat/webapps/'
                }
            }
        }
    }
}
