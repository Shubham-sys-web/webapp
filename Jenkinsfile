pipeline {
    agent any
    environment {
        SEMGREP_PATH = "/home/ubuntu/.local/bin"
    }
    stages {
        stage('Initialize') {
            steps {
                sh '''
                    echo "PATH = ${PATH}"
                    echo "M2_HOME = ${M2_HOME}"
                    export PATH=$PATH:${SEMGREP_PATH}
                    which semgrep || echo "⚠️ semgrep not found in PATH"
                    semgrep --version || true
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

        stage('SAST Scan - Semgrep') {
            steps {
                script {
                    sh '''
                        export PATH=$PATH:${SEMGREP_PATH}
                        semgrep scan \
                            --config p/owasp-top-ten \
                            --config p/secrets \
                            --config p/security-audit \
                            --json --output semgrep-report.json . || true
                    '''

                    def findingsCount = sh(
                        script: "grep -o '\"check_id\"' semgrep-report.json | wc -l",
                        returnStdout: true
                    ).trim()

                    echo "🔎 Semgrep: ${findingsCount} findings detected (SQLi/Path Traversal/XSS/etc.)"

                    archiveArtifacts artifacts: 'semgrep-report.json', allowEmptyArchive: true

                    if (findingsCount.toInteger() > 0) {
                        echo "🚨 SAST vulnerabilities found in source code! Build blocked."
                        error "Pipeline stopped: ${findingsCount} SAST vulnerabilities found. Fix code before deploying."
                    } else {
                        echo "✅ No SAST findings. Proceeding to build."
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
