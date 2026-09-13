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
                    sh 'trufflehog --json --regex --entropy=False https://github.com/Shubham-sys-web/webapp.git > trufflehog-report.json || true'
                    def findings = readFile('trufflehog-report.json').trim()
                    if (findings.length() > 0) {
                        echo "🚨 SECRETS DETECTED by TruffleHog:"
                        sh 'cat trufflehog-report.json'
                        error "Build aborted due to detected secrets."
                    } else {
                        echo "✅ No secrets detected."
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
                    sh 'scp -o StrictHostKeyChecking=no target/*.war ubuntu@13.233.2.128:/opt/tomcat/webapps/'
                }
            }
        }
    }
}
