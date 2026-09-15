pipeline {
    agent any
    environment {
        SEMGREP_PATH = "/home/ubuntu/.local/bin"
        SNYK_PATH    = "/usr/local/bin"
    }
    stages {
        stage('Initialize') {
            steps {
                sh '''
                    echo "PATH = ${PATH}"
                    export PATH=$PATH:${SEMGREP_PATH}
                    which semgrep
                    semgrep --version
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

        stage('SCA Scan - Snyk') {
            steps {
                withCredentials([string(credentialsId: 'snyk-token', variable: 'SNYK_TOKEN')]) {
                    script {
                        sh '''
                            export PATH=$PATH:${SNYK_PATH}
                            which snyk
                            snyk --version
                        '''

                        // Full JSON report (non-blocking so we always get the report file)
                        sh '''
                            export PATH=$PATH:${SNYK_PATH}
                            snyk test --json > snyk-report.json || true
                        '''
                        sh 'cat snyk-report.json'

                        archiveArtifacts artifacts: 'snyk-report.json', allowEmptyArchive: true

                        // Gate the pipeline on high/critical severity
                        def snykExitCode = sh(
                            script: '''
                                export PATH=$PATH:${SNYK_PATH}
                                snyk test --severity-threshold=high
                            ''',
                            returnStatus: true
                        )

                        if (snykExitCode != 0) {
                            echo "🚨 SCA VULNERABILITIES DETECTED by Snyk! Build aborted."
                            error "Pipeline stopped: high/critical severity vulnerabilities found in dependencies (see snyk-report.json)."
                        } else {
                            echo "✅ No high/critical SCA vulnerabilities found. Proceeding to build."
                        }
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
                            --json --output semgrep-report.json .
                    '''

                    if (!fileExists('semgrep-report.json')) {
                        error "Pipeline stopped: Semgrep did not run — report file missing."
                    }

                    def findingsCount = sh(
                        script: "grep -o '\"check_id\"' semgrep-report.json | wc -l",
                        returnStdout: true
                    ).trim()

                    echo "🔎 Semgrep: ${findingsCount} findings detected"

                    // ---- Detailed verbose report banaya jaa raha hai ----
                    sh '''
                        which jq || sudo apt-get install -y jq
                        echo ""
                        echo "========================================================"
                        echo "           🚨 DETAILED SAST VULNERABILITY REPORT 🚨"
                        echo "========================================================"
                        jq -r '.results[] |
                          "\\n--------------------------------------------------------\\n" +
                          "🔴 Vulnerability : " + (.check_id | split(".") | last) +
                          "\\n📄 File          : " + .path +
                          "\\n📍 Line          : " + (.start.line|tostring) +
                          "\\n⚠️  Severity      : " + (.extra.severity // "N/A") +
                          "\\n📝 Message       : " + (.extra.message // "N/A" | gsub("\\n"; " "))
                        ' semgrep-report.json
                        echo "========================================================"
                    '''

                    archiveArtifacts artifacts: 'semgrep-report.json', allowEmptyArchive: true

                    if (findingsCount.toInteger() > 0) {
                        error "Pipeline stopped: ${findingsCount} SAST vulnerabilities found. Fix code before deploying (see log above for details)."
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
                    sh 'scp -o StrictHostKeyChecking=no target/*.war ubuntu@13.203.231.60:/opt/tomcat/webapps/'
                }
            }
        }
    }
}
