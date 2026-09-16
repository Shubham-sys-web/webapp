pipeline {
    agent any
    environment {
        SEMGREP_PATH = "/home/ubuntu/.local/bin"
        SNYK_PATH    = "/usr/local/bin"
        ZAP_PATH     = "/opt/zaproxy"
        DEPLOY_HOST  = "13.204.66.212"
        APP_URL      = "http://localhost:8081/WebApp/"
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

                    archiveArtifacts artifacts: 'trufflehog-report.json', allowEmptyArchive: true

                    def report = readFile('trufflehog-report.json')
                    if (report.contains('"DetectorName"')) {
                        echo "🚨 SECRETS DETECTED by TruffleHog! Marking build UNSTABLE (not aborting)."
                        unstable "Hardcoded secrets found in repository (see trufflehog-report.json)."
                    } else {
                        echo "✅ No secrets detected."
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

                        sh '''
                            export PATH=$PATH:${SNYK_PATH}
                            snyk test --json > snyk-report.json || true
                        '''
                        sh 'cat snyk-report.json'

                        archiveArtifacts artifacts: 'snyk-report.json', allowEmptyArchive: true

                        def snykExitCode = sh(
                            script: '''
                                export PATH=$PATH:${SNYK_PATH}
                                snyk test --severity-threshold=high
                            ''',
                            returnStatus: true
                        )

                        if (snykExitCode != 0) {
                            echo "🚨 SCA VULNERABILITIES DETECTED by Snyk! Marking build UNSTABLE (not aborting)."
                            unstable "High/critical severity vulnerabilities found in dependencies (see snyk-report.json)."
                        } else {
                            echo "✅ No high/critical SCA vulnerabilities found."
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
                            --json --output semgrep-report.json . || true
                    '''

                    archiveArtifacts artifacts: 'semgrep-report.json', allowEmptyArchive: true

                    if (!fileExists('semgrep-report.json')) {
                        echo "⚠️ Semgrep did not produce a report — skipping findings check."
                    } else {
                        def findingsCount = sh(
                            script: "grep -o '\"check_id\"' semgrep-report.json | wc -l",
                            returnStdout: true
                        ).trim()

                        echo "🔎 Semgrep: ${findingsCount} findings detected"

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

                        if (findingsCount.toInteger() > 0) {
                            echo "🚨 ${findingsCount} SAST vulnerabilities found. Marking build UNSTABLE (not aborting)."
                            unstable "${findingsCount} SAST vulnerabilities found (see log/report above)."
                        } else {
                            echo "✅ No SAST findings."
                        }
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
                    sh "scp -o StrictHostKeyChecking=no target/*.war ubuntu@${DEPLOY_HOST}:/opt/tomcat/webapps/"
                }
            }
        }

        stage('DAST Scan - OWASP ZAP') {
            steps {
                script {
                    sh 'sleep 25'

                    // Kill any stray ZAP process left over from a previous failed run,
                    // so it doesn't hold onto the proxy port.
                    sh 'pkill -f zap.sh || true'
                    sh 'sleep 3'

                    sh """
                        ${ZAP_PATH}/zap.sh -cmd \
                          -port 8090 \
                          -quickurl ${APP_URL} \
                          -quickprogress \
                          -quickout \$WORKSPACE/zap-report.html || true
                    """

                    archiveArtifacts artifacts: 'zap-report.html', allowEmptyArchive: true

                    if (fileExists('zap-report.html')) {
                        def zapReport = readFile('zap-report.html')
                        def highCount = zapReport.count('Risk Level: High')

                        if (highCount > 0) {
                            echo "🚨 ZAP found ${highCount} High-risk alert(s)! Marking build UNSTABLE (not aborting)."
                            unstable "OWASP ZAP found ${highCount} High-risk vulnerabilities on deployed app (see zap-report.html)."
                        } else {
                            echo "✅ No High-risk alerts found by ZAP."
                        }
                    } else {
                        echo "⚠️ ZAP report not generated — check ZAP installation/logs."
                    }
                }
            }
        }
    }

    post {
        always {
            echo "Pipeline finished. Result: ${currentBuild.result ?: 'SUCCESS'}"
        }
    }
}
