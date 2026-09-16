# DevSecOps-Project — webapp CI/CD Pipeline

In this project, I built an end-to-end CI/CD pipeline on AWS EC2 while keeping Security Best Practices and DevSecOps principles in mind — using **Git, GitHub, Jenkins, Maven, TruffleHog, Snyk, Semgrep, Apache Tomcat, and OWASP ZAP** to achieve the goal of shipping code that is built, security-scanned at every layer, and automatically deployed, all in one pipeline.

Show Image

---

## 📋 Table of Contents

- Architecture
- Pipeline Flow
- Prerequisites
- Build This Project Yourself
- Tools Used
- Stage-by-Stage Breakdown
- Full Jenkinsfile
- Screenshots
- Documentation
- Issues Faced & Fixes
- Security Notes
- Next Steps

## ARCHITECTURE

## **Project Architecture**

!ChatGPT Image Sep 16, 2026, 01_59_23 PM.png

## 🔄 Pipeline Flow

!image.png

This mirrors the pipeline as it actually runs in Jenkins:

`Initialize → Secret Scan (TruffleHog) → SCA Scan (Snyk) → SAST Scan (Semgrep) → Build (Maven) → Deploy To Tomcat → DAST Scan (OWASP ZAP)`

**Step-by-step, with what happens on failure:**

1. **Jenkins fetches the code** from the GitHub repository — triggered automatically via a GitHub webhook, with Poll SCM as a fallback.
2. **TruffleHog scans the repository** for hardcoded secrets and credentials. If a real secret is detected, the stage aborts the pipeline immediately and Jenkins reports the failure — Build and Deploy never run.
3. **Snyk scans the project's Maven dependencies** (`pom.xml`) for known CVEs (SCA). Any High/Critical severity finding is reported and the build is marked accordingly, so the vulnerability is visible before it reaches production.
4. **Semgrep statically analyzes the Java source code** (SAST) against OWASP Top 10, secrets, and security-audit rulesets, printing file/line/severity for every finding directly in the console log.
5. **Maven builds the application** (`mvn clean package`). If the build fails, the whole pipeline fails and Jenkins notifies the user — none of the later stages run.
6. **Jenkins deploys the built WAR to the Tomcat server** over SSH (`sshagent` + `scp` into `webapps/`, which Tomcat auto-deploys within seconds). If the deploy step fails, the pipeline fails and Jenkins notifies the user.
7. **OWASP ZAP scans the live, deployed application** (DAST) with a Quick Scan (spider + active scan). Any High-risk finding is reported in the console and the full HTML report is archived as a Jenkins build artifact for review.

## ✅ Prerequisites

1. JDK 21 (Jenkins server) / JDK 17 (Tomcat server)
2. Git
3. A GitHub account and repository
4. Jenkins (with Blue Ocean and SSH Agent / Publish over SSH plugins)
5. Maven
6. TruffleHog
7. A Snyk account (free tier) + Snyk CLI
8. Semgrep (`pipx install semgrep`)
9. Apache Tomcat 9
10. OWASP ZAP
11. Two AWS EC2 instances (Jenkins + Tomcat) in the same VPC / Security Group

## 1-Jenkins_Tomcat_Setup_Guide 
https://github.com/Shubham-sys-web/webapp/blob/master/1-Jenkins_Tomcat_Setup_Guide%20.pdf

# 2-Continuous_Integration_Jenkins_POC

# **DevSecOps-Project**

In this project, I created an end-to-end CI/CD pipeline while keeping in mind Securities Best Practices, DevSecOps principles and used all these tools *Git, GitHub , Jenkins,Maven, Junit, SonarQube, Docker, Trivy, AWS S3, Docker Hub, Kubernetes , Slack and Hashicorp Vault,* to achive the goal.

## **Project Architecture**

!ChatGPT Image Sep 16, 2026, 01_59_23 PM.png

**Continuous Integration with Jenkins**

Creating a Build Pipeline: Jenkins + GitHub + Maven

*A Step-by-Step Proof of Concept (POC) Guide*

Document Overview

This document walks through the practical steps of setting up Continuous Integration (CI) using Jenkins, connected to a GitHub repository, with Maven as the build tool. Each step is illustrated with an actual screenshot taken during the setup, followed by a detailed explanation of what was done and why.

Scope

- Creating a new Pipeline job in Jenkins
- Configuring the job to connect with a GitHub repository
- Writing and committing a Jenkinsfile that defines the build stages
- Running the pipeline and diagnosing a build failure
- Fixing the Jenkins Tools configuration
- Re-running the pipeline to a successful build

Table of Contents

- 1. Reference Slide — Creating Build Pipeline in Jenkins
- 2. Step 1 — Start a New Item in Jenkins
- 3. Step 2 — Name the Job and Select 'Pipeline' Type
- 4. Step 3 — General Configuration and Build Retention
- 5. Step 4 — Link the GitHub Project and Configure Triggers
- 6. Step 5 — Configure the Pipeline Definition (Pipeline script from SCM)
- 7. Step 6 — Locate the Repository on GitHub
- 8. Step 7 — Create the Jenkinsfile in the Repository
- 9. Step 8 — First Build Attempt and Failure Diagnosis
- 10. Step 9 — Root Cause: Missing Jenkinsfile Case Sensitivity
- 11. Step 10 — Fixing Global Tool Configuration (Git)
- 12. Step 11 — Triggering the Build Again
- 13. Step 12 — Successful Build Confirmation
- 14. Step 13 — Reviewing the Full Pipeline Run in Blue Ocean
- 15. Summary and Key Takeaways

1. Reference Slide — Creating Build Pipeline in Jenkins

The following reference slide outlines the standard five-step process for creating a build pipeline in Jenkins, which this POC follows: logging in to the Jenkins UI, creating a new Pipeline item, connecting to a source code repository, creating a Jenkinsfile that defines the Build stage, and finally running the pipeline. The adjacent code snippet shows the Jenkinsfile structure used, with an 'Initialize' stage that prints environment variables and a 'Build' stage that runs the Maven package command.

![](attachment:2331f214-708a-4fae-8734-8d5dac8c7b6b:e722b28d480b68ea3f0203b5fc227c2e34fcaf85.png)

*Reference slide: standard steps for creating a build pipeline in Jenkins, alongside the Jenkinsfile stage structure.*

2. Step 1 — Start a New Item in Jenkins

After logging in to the Jenkins dashboard at http://localhost:8080 (accessed through an SSH tunnel), the first action is to click on New Item in the left-hand navigation panel. This is the entry point for creating any job in Jenkins, whether it is a Freestyle project, a Pipeline, or another job type.

![](attachment:3c96832c-0114-4808-9af5-7a944f4ad87f:cb1b58664953db44a7e9552aac5e6d5caad6b8ce.png)

*The Jenkins dashboard homepage. 'New Item' is highlighted as the starting point for creating a pipeline job.*

3. Step 2 — Name the Job and Select 'Pipeline' Type

On the New Item screen, a name is entered for the job — in this case, webapp-cicd-pipeline. Jenkins then presents several item types to choose from: Pipeline, Freestyle project, Maven project, and Multi-configuration project. Pipeline is selected because it allows the entire build process to be defined as code (a Jenkinsfile), supports multiple stages, and can run across different agents. Clicking OK creates the job and proceeds to its configuration screen.

![](attachment:850f2038-2039-4939-bf28-8f4f1012c4e4:9e271db1b32d8ab40731eeb02ea25768b2168ebd.png)

*Naming the new item 'webapp-cicd-pipeline' and selecting the Pipeline project type.*

4. Step 3 — General Configuration and Build Retention

The job configuration screen opens with the General tab active. A short description, devsecops-pipeline-demo, is added for documentation purposes. The Discard old builds option is enabled with a Log Rotation strategy, and Max # of builds to keep is set to 2. This prevents the Jenkins workspace and build history from growing indefinitely and consuming disk space on the t3.micro instance, which has limited storage — a precaution that proved directly relevant later when disk space issues were encountered during pipeline execution.

![](attachment:633abbcf-ab73-49ea-8149-908fddd4ee76:59a570fa35d3ca058eddd95129f87feb28ea262a.png)

*General configuration tab: description added and build retention set to keep only the last 2 builds.*

5. Step 4 — Link the GitHub Project and Configure Triggers

Scrolling down, the GitHub project checkbox is enabled and the Project URL is set to the repository being built: https://github.com/Shubham-sys-web/webapp.git. This associates the job with the GitHub project for display purposes and enables GitHub-specific features.

In the Triggers section, two options are enabled:

- GitHub hook trigger for GITScm polling — allows GitHub to notify Jenkins immediately when new code is pushed, so a build can start automatically (via a webhook).
- Poll SCM — as a fallback, Jenkins periodically checks the repository itself for new commits and triggers a build if changes are found, even if the webhook notification does not arrive.

![](attachment:a62702ec-ca2e-4ac2-8413-78ba140b8cea:c0f7475180c4495df2b95617bbf3407b75c1e712.png)

*GitHub project URL configured, with both GitHub hook trigger and Poll SCM enabled for automatic build triggering.*

6. Step 5 — Configure the Pipeline Definition (Pipeline script from SCM)

In the Pipeline section, the Definition dropdown is set to Pipeline script from SCM. This tells Jenkins that the pipeline's instructions (the Jenkinsfile) live inside the source code repository itself, rather than being written directly into the Jenkins job configuration. This is the recommended approach because it keeps the pipeline definition version-controlled alongside the application code.

The SCM is set to Git, and the same Repository URL (https://github.com/Shubham-sys-web/webapp.git) is entered again here. Since the repository is public, Credentials is left as - none -. The Branch Specifier is left at its default value, */master, which tells Jenkins to build from the master branch. After these settings are confirmed, Apply and then Save are clicked to store the configuration.

![](attachment:34d186cb-21c7-41f7-bf7a-a96fa1d84caa:edeabb99b971f82db3ab59057bf077ea1454a12b.png)

*Pipeline definition set to 'Pipeline script from SCM', pointing to the Git repository and the master branch.*

7. Step 6 — Locate the Repository on GitHub

Switching to the GitHub side, the webapp repository (a fork of cehkunal/webapp) is opened. This is where the Jenkinsfile needs to be created, since the Jenkins job was just configured to pull its pipeline definition from this repository's source control. The Add file dropdown is used, and Create new file is selected.

![](attachment:3bdaffa2-9271-47f9-92cd-08a181254f4d:d987caf53422f02534847a08e6c48b6bab9a7d5f.png)

*The webapp GitHub repository, with 'Add file' → 'Create new file' selected to add the Jenkinsfile.*

8. Step 7 — Create the Jenkinsfile in the Repository

A new file is created directly in the GitHub web editor. The file's content defines the pipeline structure using Declarative Pipeline syntax:

```jsx
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

        stage('Build') {

            steps {

                sh 'mvn clean package'
            }
        }

        stage('SCA Scan - Snyk') {

            steps {

                withCredentials([
                    string(
                        credentialsId: 'snyk-token',
                        variable: 'SNYK_TOKEN'
                    )
                ]) {

                    script {

                        // Verify Snyk installation
                        sh '''
                            export PATH=$PATH:${SNYK_PATH}

                            which snyk
                            snyk --version
                        '''

                        // Generate JSON report
                        sh '''
                            export PATH=$PATH:${SNYK_PATH}

                            snyk test --json > snyk-report.json || true
                        '''

                        // Display report
                        sh 'cat snyk-report.json'

                        // Archive report
                        archiveArtifacts(
                            artifacts: 'snyk-report.json',
                            allowEmptyArchive: true
                        )

                        // Security Gate
                        def snykExitCode = sh(
                            script: '''
                                export PATH=$PATH:${SNYK_PATH}

                                snyk test --severity-threshold=high
                            ''',
                            returnStatus: true
                        )

                        if (snykExitCode != 0) {

                            error "Pipeline stopped: high/critical severity vulnerabilities found."

                        } else {

                            echo "No high/critical SCA vulnerabilities found."
                        }
                    }
                }
            }
        }
    }
}
```

This defines two stages: Initialize, which prints the current PATH and M2_HOME environment variables for diagnostic purposes, and Build, which runs mvn clean package — the standard Maven command that compiles the source code, runs tests, and packages the application into a deployable artifact (a .war file, since this is a web application). The change is committed directly to the master branch using the Commit changes button.

![](attachment:8baf9a47-d3f3-4bb7-a115-4b1757b5e611:d81cd2e957e67ef0f93099ce9997c6069526ef83.png)

*Creating the Jenkinsfile in the GitHub web editor with the Initialize and Build stages, ready to commit.*

9. Step 8 — First Build Attempt and Failure Diagnosis

Back in Jenkins, build #1 was triggered automatically by the SCM change (the Jenkinsfile commit). However, the build status shows a red cross icon next to #1 (Sep 11, 2026, 7:16:09 AM), indicating failure. To investigate, Console Output is opened from the left-hand menu — this is the standard first troubleshooting step for any failed Jenkins build, as it shows the exact log output produced during the run.

![](attachment:778f4c53-51cf-48da-84ff-df3127824196:113b8ed6a2949650ae813510bbb89296995a57f0.png)

*Build #1 failed (red cross icon). Console Output is selected to view the failure details.*

10. Step 9 — Root Cause: Missing Jenkinsfile Case Sensitivity

The console output reveals the exact error:

Started by an SCM change

ERROR: Unable to find Jenkinsfile from git https://github.com/Shubham-sys-web/webapp.git

Finished: FAILURE

Jenkins, by default, looks for a file named exactly Jenkinsfile (capital J) in the repository root. Investigation of the repository showed the file had been committed as jenkinsfile (lowercase j). Since Git and Linux filesystems are case-sensitive, Jenkins could not locate the file under the name it expected, even though a file with a similar name existed.

**Note:** The fix applied was to rename the file to the exact expected case, Jenkinsfile, directly on GitHub. Alternatively, the Script Path field in the job's Pipeline configuration could have been changed to match the lowercase filename instead — renaming the file was chosen as it follows the standard Jenkins convention.

![](attachment:09bacc3b-bbcf-4eed-b49e-231a98898244:700ee7546b6b49c46ee86973d0b8367e58e78cde.png)

*Console output showing the 'Unable to find Jenkinsfile' error — caused by a filename case mismatch.*

11. Step 10 — Fixing Global Tool Configuration (Git)

While investigating the build environment, the Git tool configuration was also reviewed under Manage Jenkins → Tools (accessible at /manage/configureTools/). Under Git installations, the Install automatically option is enabled, which allows Jenkins to automatically download and manage the Git executable rather than depending on a specific pre-installed path. This ensures the pipeline's Git operations (cloning the repository, checking out branches) work reliably regardless of what is or is not already installed on the underlying instance.

![](attachment:c2d25525-a85e-4daf-a5e3-c3f9feb165ba:2533fd3c0f7750230967a8fa6040c1a94803bc7d.png)

*Manage Jenkins → Tools: Git installation configured with 'Install automatically' enabled.*

12. Step 11 — Triggering the Build Again

With the Jenkinsfile renamed correctly, the pipeline job page is opened again and Build Now is clicked from the left-hand menu to manually trigger a new run. The Builds panel at the bottom shows the history: build #1 and #2 both failed (red icons), while build #3, just started at 7:42 AM, is shown as actively running (red progress bar), confirming that this time the pipeline is executing rather than failing immediately.

![](attachment:a2bbd950-1941-4a35-861b-8495a290d6a4:2ec3f59d1267c37e936d61edd496af3a2089dd2c.png)

*Manually triggering build #3 with 'Build Now'. The build is shown actively running, unlike the earlier failed attempts.*

13. Step 12 — Successful Build Confirmation

Once build #3 completes, its status page confirms success with a green checkmark: #3 (Sep 11, 2026, 7:42:13 AM), started by user admin. The Git Build Data section confirms the exact commit that was built (revision 7f273e55727b41a8798513655fec9c254ba0ec61) and the branch reference (refs/remotes/origin/master), with No changes noted for this particular run since it was manually triggered rather than by a new commit.

![](attachment:6df78ed5-38f4-4bc6-9725-def704276c42:d29b3ab159739a29cb661eaa87e8d243faa7086e.png)

*Build #3 completed successfully (green checkmark), with Git revision and repository details confirmed.*

14. Step 13 — Reviewing the Full Pipeline Run in Blue Ocean

Opening the build in Blue Ocean (Jenkins' modern visual pipeline interface) provides a clearer, graphical view of the pipeline's stages: Start → Initialize → Build → End, all marked with green checkmarks. The Build stage log below shows Maven downloading its dependencies from Maven Central (repo.maven.apache.org) — plugin POMs and JARs required to compile and package the project — followed by the packaging of the WAR file and a final BUILD SUCCESS message. The total run took 34 minutes on this occasion (a first-time run against a t3.micro instance, before Maven's local dependency cache was warm); subsequent builds are expected to complete significantly faster since dependencies are cached locally after the first successful download.

![](attachment:a2bbd950-1941-4a35-861b-8495a290d6a4:2ec3f59d1267c37e936d61edd496af3a2089dd2c.png)

*Blue Ocean pipeline view showing the four stages of the run — Start, Initialize, Build, End — each completed successfully.*

15. Summary and Key Takeaways

This POC demonstrates a complete, working Continuous Integration setup using Jenkins Pipeline as Code, sourced directly from a GitHub repository. The key steps performed were:

**1.** Created a Pipeline-type job in Jenkins and linked it to a GitHub repository via GitHub project settings and SCM polling/webhook triggers.

**2.** Configured the pipeline definition as 'Pipeline script from SCM', keeping the build logic version-controlled with the application code rather than hardcoded into the Jenkins UI.

**3.** Authored a Jenkinsfile with two stages — Initialize (environment diagnostics) and Build (Maven packaging) — and committed it to the repository.

**4.** Diagnosed and resolved a build failure caused by filename case sensitivity (jenkinsfile vs. Jenkinsfile), using Jenkins' Console Output as the primary debugging tool.

**5.** Verified the Git tool configuration in Manage Jenkins → Tools to ensure reliable source control operations.

**6.** Re-triggered and confirmed a successful build, validating the pipeline end-to-end from source checkout through to a packaged WebApp.war artifact.

**Note:** A recurring lesson from this exercise is that Jenkins error messages and Console Output logs are precise and should be read carefully — nearly every failure encountered (missing Jenkinsfile, GPG key mismatch, insufficient Java version, malformed XML, low disk space) was diagnosable directly from the log text, without needing external troubleshooting.

# 3-Continuous_Deployment_Tomcat_POC

**Continuous Deployment to Tomcat**

Integrating Automated Deployment into the Jenkins Pipeline

*A Step-by-Step Proof of Concept (POC) Guide*

Document Overview

This document builds on the earlier Continuous Integration (CI) setup by adding Continuous Deployment (CD): once a build passes, the packaged WAR artifact is automatically copied to the Tomcat server and deployed, with no manual step required. Each step below is illustrated with an actual screenshot from the setup, followed by a detailed explanation.

Scope

- Reference process for integrating automated deployment into a Jenkins pipeline
- Creating SSH credentials in Jenkins for the Tomcat (production) server
- Preparing the Tomcat webapps directory with the correct permissions
- Adding a Deploy-to-Tomcat stage to the Jenkinsfile using the SSH Agent plugin
- Running the pipeline and verifying a successful end-to-end deployment
- Confirming the deployed application is live and reachable

Table of Contents

- 1. Reference Slide — Integrating Automated Deployment to Tomcat
- 2. Step 1 — Open Credentials and Add New Credentials
- 3. Step 2 — Fill in the Tomcat SSH Credential Details
- 4. Step 3 — Confirm the Credential Was Created
- 5. Step 4 — Prepare Write Permissions on the Tomcat webapps Directory
- 6. Step 5 — Add the Deploy-to-Tomcat Stage in the Jenkinsfile
- 7. Step 6 — Trigger the Pipeline Build
- 8. Step 7 — Build #15 Completes Successfully
- 9. Step 8 — Application Verified Live on Tomcat
- 10. Step 9 — Reviewing the Deploy Stage in Blue Ocean
- 11. Summary and Key Takeaways

1. Reference Slide — Integrating Automated Deployment to Tomcat

The reference slide below outlines the standard process for adding automated deployment to a Jenkins pipeline: once the build passes, the artifact should be deployed to the production Tomcat server; the Deploy to Container and SSH Agent plugins need to be installed; SSH credentials for the production server must be created in Jenkins; and finally a Deploy-To-Tomcat stage is added to the pipeline. The accompanying code snippet shows the target Deploy-To-Tomcat stage structure using sshagent and scp.

![](attachment:863fe5b6-b043-4316-aa57-13bfaf76ecef:9f2a7c5051c6c70d59257b30aa10d80cfda49bac.png)

*Reference slide: steps for integrating automated deployment to Tomcat, with the target Jenkinsfile stage shown below.*

2. Step 1 — Open Credentials and Add New Credentials

Deployment to the Tomcat server requires Jenkins to authenticate over SSH. The first step is to navigate to Manage Jenkins -> Credentials (http://localhost:8080/manage/credentials/) and open the Add Credentials dialog. Several credential types are available; SSH Username with private key is selected, since Jenkins will connect to the Tomcat instance the same way a developer would from the terminal, using the EC2 key pair rather than a password.

![](attachment:c8a6090f-f05b-4ece-b296-6445f61c2b0a:b786818828d67e9a51851f82a93dcf6c94312117.png)

*The Add Credentials dialog in Jenkins, with 'SSH Username with private key' selected as the credential type.*

3. Step 2 — Fill in the Tomcat SSH Credential Details

On the credential creation form, the following values are entered:

- ID: tomcat, a short identifier used later in the Jenkinsfile to reference this credential.
- Description: tomcat ssh credentials, a human-readable label shown in the credentials list.
- Username: ubuntu, the OS user on the Tomcat EC2 instance.
- Private Key: 'Enter directly' is selected, and the full contents of the .pem key file used to SSH into the Tomcat server are pasted into the Key field.

Once all fields are filled in, the Create button is clicked to save the credential.

![](attachment:d52ce886-37f6-44a6-9f61-9721c27317bd:fe3d82f60b93fb909da392de5ae218ebf55454e2.png)

*Filling in the SSH credential form: ID 'tomcat', username 'ubuntu', and the private key pasted directly.*

4. Step 3 — Confirm the Credential Was Created

After saving, the Credentials page lists the new entry: tomcat, associated with the username ubuntu, under System -> Global, with the description 'tomcat ssh credentials'. This confirms the credential is stored and available for any pipeline job to reference by its ID.

![](attachment:c78e66fb-e504-4354-98bd-40c4196c8e63:5e8e08472479c4052b8a887aed312d4dada92ed5.png)

*The Credentials list confirming the 'tomcat' SSH credential has been created successfully.*

5. Step 4 — Prepare Write Permissions on the Tomcat webapps Directory

Before Jenkins can copy a WAR file into Tomcat's webapps directory over SCP, the ubuntu user connecting via SSH needs write access to that folder. Running ls -la in /opt/tomcat initially shows webapps with permissions drwxr-x---, which does not allow write access for the deploying user in this context. To resolve this, chmod 777 webapps/ is run, after which the directory shows as drwxrwxrwx, writable by any user.

**Caution:** Setting permissions to 777 (read/write/execute for everyone) is a quick fix for a POC/demo environment, but it is not a good security practice for production. A better long-term approach is to set ownership of the webapps directory to the specific deploying user (chown ubuntu:ubuntu webapps/) or use a dedicated deployment group with 775 permissions, so write access is not open to all users on the system.

![](attachment:6acc111e-9a47-4f5f-bfaf-0a883b6b8519:67e6c1e65b9f46c9e7b25a2e3d38b81d85931f92.png)

*Changing permissions on /opt/tomcat/webapps to allow the deployment user to write the WAR file (777 used for this POC; noted as not ideal for production).*

6. Step 5 — Add the Deploy-to-Tomcat Stage in the Jenkinsfile

With the credential and target directory ready, a third stage is added to the existing Jenkinsfile (after Initialize and Build):

```jsx
stage('Deploy To Tomcat') {

    steps {

        sshagent(['tomcat']) {

            sh '''
                scp -o StrictHostKeyChecking=no \
                target/*.war \
                ubuntu@13.233.2.128:/opt/tomcat/webapps/
            '''
        }
    }
}
```

The sshagent(['tomcat']) block loads the previously created SSH credential (matching it by the ID 'tomcat') and makes it available to the shell step inside. The scp command then copies the built WAR file, matched with the wildcard target/*.war, to the Tomcat server's webapps directory over SSH, with host key checking disabled (-o StrictHostKeyChecking=no) so the first connection does not require interactive confirmation. This change is committed directly to the Jenkinsfile in the GitHub repository.

![](attachment:99af4dc7-1b06-42fa-9edb-d3f516229293:f3ea4e8239a19dbb602c10137f93575dc25d1759.png)

*The completed Jenkinsfile with the Deploy To Tomcat stage added, using sshagent and scp to copy the WAR file to the Tomcat server.*

7. Step 6 — Trigger the Pipeline Build

Back in the Jenkins job (webapp-cicd-pipeline), Build Now is clicked to manually trigger a new run against the updated Jenkinsfile. The job's permalinks panel shows the history of previous runs, build #3 as the last successful and last stable build at this point, confirming that this new run will be tracked as the pipeline evolves to include deployment.

![](attachment:953c8c71-3e16-4e58-bc8a-23ac34e4188c:ff341e0de2cb61c56c8185bb1321f13c555ae97a.png)

*Triggering a new build with 'Build Now' from the Jenkins job page, after committing the updated Jenkinsfile.*

8. Step 7 — Build #15 Completes Successfully

Build #15 completes with a green checkmark, confirming success. The Git Build Data section shows the exact commit that introduced the deployment stage (revision d2d61d6fa505357f0d96c1788c9a2ee7c2f3eab2, commit message 'Update Jenkinsfile', on refs/remotes/origin/master). This is the first build to include all three stages: Initialize, Build, and Deploy To Tomcat.

![](attachment:50457e93-42b4-48c3-b5a5-9c42be99f85d:83b10e7df55c9f8bd6202f41dcc24140227c51d0.png)

*Build #15 completed successfully, showing the Jenkinsfile update commit that added the deployment stage.*

9. Step 8 — Application Verified Live on Tomcat

To confirm the deployment worked end-to-end, the application is opened through the SSH tunnel at http://localhost:8081/WebApp/, matching the exact case of the WAR filename (WebApp.war), since Tomcat's context path is case-sensitive. The page loads successfully, displaying 'Web App for Developers' with sample content including a sign-in bar, a hero section reading 'Hello - This is from dev branch', and three content cards. This confirms the WAR file was correctly transferred and auto-deployed by Tomcat without any manual intervention.

![](attachment:14d62b10-89a2-4a74-ae5e-01a38f355a3f:288255363c8553d3314c6d80ff847b1f3fc7e085.png)

*The deployed application live in the browser at localhost:8081/WebApp/, confirming the end-to-end pipeline worked.*

10. Step 9 — Reviewing the Deploy Stage in Blue Ocean

Opening build #15 in Blue Ocean shows all four stages, Start, Initialize, Build, Deploy To Tomcat, End, marked with green checkmarks. The Deploy To Tomcat stage log shows the exact command executed:

scp -o StrictHostKeyChecking=no target/*.war ubuntu@13.233.2.128:/opt/tomcat/webapps/

Warning: Permanently added '13.233.2.128' (ED25519) to the list of known hosts.

The stage completed in under 1 second, and the 'Warning: Permanently added...' line is expected, informational output from SSH recording the host's key on first connection, not an error. This confirms the full Continuous Integration and Continuous Deployment pipeline is working end-to-end.

![](attachment:c9ffff50-b29e-4ac4-8f7c-63d2afc7f149:bdf317c7ad6150d64c4326cb57d1912634eb3ad7.png)

*Blue Ocean view of build #15: all four stages completed successfully, with the Deploy To Tomcat step log visible.*

11. Summary and Key Takeaways

This POC extends the earlier CI pipeline into a full CI/CD pipeline by adding automated deployment. The key steps performed were:

- Created a dedicated SSH credential in Jenkins (ID: tomcat) using the Tomcat server's .pem private key, so Jenkins can authenticate without a password.
- Prepared the Tomcat webapps directory with write permissions so the deploying user can copy files into it.
- Added a Deploy To Tomcat stage to the Jenkinsfile using the SSH Agent plugin and an scp command to transfer the built WAR file.
- Triggered and verified a successful build (#15) that ran all three stages, Initialize, Build, Deploy, end-to-end.
- Confirmed the deployed application was live and reachable on the Tomcat server, with no manual deployment step required.

**Note:** Tomcat auto-deploys any WAR file dropped into its webapps directory within a few seconds, without needing a restart. This is what allows the pipeline to go from 'build passed' to 'application live' with a single scp command and no additional Tomcat-side action.

# 4-TruffleHog_Jenkins_Secret_Scanning_Docs

**TruffleHog Secret Scanning**

**Integration with Jenkins CI/CD Pipeline**

*Setup Guide, Configuration Reference & Proof of Concept*

**Document purpose**

This document records the complete setup of TruffleHog (secret-scanning tool) on a Jenkins build agent, its integration into a Jenkins declarative pipeline, and a proof of concept demonstrating that the pipeline correctly blocks a build when a hardcoded AWS credential is found in the source repository.

---

**1. Overview**

TruffleHog is an open-source secret-scanning tool that searches source code repositories, commit history, and file systems for accidentally committed credentials such as AWS keys, API tokens, and private keys. In this setup, TruffleHog is installed on the Jenkins build agent and wired into the pipeline as a dedicated "Secret Scan" stage that runs before the Build and Deploy stages.

Pipeline flow used in this setup:

1. Initialize – basic environment checks (PATH, M2_HOME).
2. Secret Scan (TruffleHog) – scans the GitHub repository for hardcoded secrets.
3. Build – runs ‘mvn clean package’ (only if no secrets were found).
4. Deploy to Tomcat – copies the built WAR file to the Tomcat server over SSH (only if Build succeeded).

The key design decision: if TruffleHog reports a detected secret, the pipeline stage throws an error and the pipeline stops immediately — the Build and Deploy stages are skipped entirely. This prevents any commit containing a hardcoded credential from ever reaching the Tomcat server.

**2. Prerequisites**

- An Ubuntu-based Jenkins build agent (EC2 instance in this setup) with sudo access.
- Jenkins installed and running, with a service account named ‘jenkins’.
- Internet access on the agent to download the TruffleHog release binary.
- A GitHub repository containing the application source code.
- SSH credentials configured in Jenkins (credential ID ‘tomcat’) for deployment to the Tomcat host.

**3. Installing TruffleHog on the Jenkins Agent**

Install the TruffleHog binary system-wide so both the ‘ubuntu’ login user and the ‘jenkins’ service user can access it.

**3.1 Download and install to /usr/local/bin**

curl -sSfL https://raw.githubusercontent.com/trufflesecurity/\

trufflehog/main/scripts/install.sh | sh -s -- -b /usr/local/bin

---

This installs the binary to /usr/local/bin/trufflehog, which is on the default PATH for all system users, including the Jenkins service account.

**3.2 Verify the installation**

which trufflehog

trufflehog --version

# Confirm the Jenkins service account can also see it

sudo -u jenkins trufflehog --version

---

**3.3 Known issue: old cached path in an existing shell session**

If TruffleHog was previously installed in a user-local path (for example ~/.local/bin/trufflehog) and later reinstalled to /usr/local/bin, an already-open terminal session may still resolve the old, now-missing path. Bash caches resolved command paths for the lifetime of the session (‘hashing’).

**Symptom**

- bash: /home/ubuntu/.local/bin/trufflehog: No such file or directory

---

Fix – clear bash's cached path, or simply open a new terminal session:

hash -r

trufflehog --version

# or, in a fresh terminal:

which trufflehog

trufflehog --version

---

**3.4 Known issue: auto-updater error on scan**

TruffleHog checks for a newer release and tries to self-update on each run. If the executing user does not have write permission on the binary's location, this fails with a warning — the scan itself is unaffected.

**Symptom**

{"level":"error", ... "msg":"error occurred with trufflehog updater", "error":"cannot move binary (exit status 1)"}

---

Two ways to resolve this:

- Recommended – disable the auto-updater explicitly for every scan by adding the ‘--no-update’ flag.
- Alternative – grant the executing user ownership of the binary so it can update itself: ‘sudo chown ubuntu:ubuntu /usr/local/bin/trufflehog’. (Not used in the final Jenkinsfile, since Jenkins runs scans non-interactively and ‘--no-update’ is the cleaner, repeatable fix.)

trufflehog github --repo=https://github.com/<org>/<repo>.git --json --no-update

---

**4. Running a Manual Scan (Command Reference)**

Core command used to scan a GitHub repository and print results as JSON:

trufflehog github --repo=https://github.com/Shubham-sys-web/webapp.git --json --no-update

---

Sample output on a clean commit (no secrets present):

{"level":"info-0","msg":"running source", ...}

{"level":"info-0","msg":"Completed enumeration","num_repos":1}

{"level":"info-0","msg":"scanning repo", ...}

{"level":"info-0","msg":"finished scanning","chunks":357,"bytes":51836,

"verified_secrets":0,"unverified_secrets":0,"scan_duration":"1.96s"}

---

Sample output once a hardcoded credential is present in the repository:

{"SourceMetadata":{"Data":{"Git":{"commit":"...","file":"index.jsp",

"line":13,"repository":"https://github.com/Shubham-sys-web/webapp.git"}}},

"SourceID":1,"SourceType":7,"SourceName":"trufflehog - github",

"DetectorType":2,"DetectorName":"AWS",

"Verified":false,

"Raw":"AKIAZQ3XJ9K2LMNPQRST",

"ExtraData":{"resource_type":"Access key"}}

---

The presence of the ‘DetectorName’ field is what indicates a real detection — this is the field the Jenkins pipeline logic checks for.

**5. Important Note: Choosing a Test Secret**

**Why the AWS documentation example key does not get flagged**

AKIAIOSFODNN7EXAMPLE is AWS's own official placeholder key, used across their documentation, SDKs, and countless tutorials. Because it appears so widely as a known, non-sensitive placeholder, TruffleHog (and other scanners such as Gitleaks and git-secrets) explicitly ignore it as a documented false positive to avoid noisy alerts. This is expected, correct scanner behaviour — not a bug.

---

For testing purposes, use a fake key that matches AWS's real key format (AKIA + 16 alphanumeric characters) but is not a published placeholder, for example:

<!--

TEST SECRET FOR TRUFFLEHOG SCANNING - DO NOT USE IN PRODUCTION

AWS_ACCESS_KEY_ID=AKIAZQ3XJ9K2LMNPQRST

AWS_SECRET_ACCESS_KEY=k3F9mP2xQ8vN5tR7wY1zA4bC6dE0gH3jK5lM8nO2

- ->

---

This format-matches a real AWS key closely enough for the detector's pattern and entropy checks to trigger, while being a unique string that is not on any scanner's ignore list. Because the key is fake, it will show up as an unverified secret (TruffleHog cannot verify it against the live AWS API), which is the expected and desired result for a test.

**6. Jenkins Pipeline Configuration**

The full declarative Jenkinsfile below integrates the secret scan as a gating stage ahead of Build and Deploy.

```jsx
```groovy
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

                    // Run TruffleHog secret scan
                    sh '''
                        trufflehog github \
                        --repo=https://github.com/Shubham-sys-web/webapp.git \
                        --json \
                        --no-update \
                        > trufflehog-report.json || true
                    '''

                    // Display scan report
                    sh 'cat trufflehog-report.json'

                    // Read report
                    def report = readFile('trufflehog-report.json')

                    // Security gate
                    if (report.contains('"DetectorName"')) {

                        echo "SECRETS DETECTED by TruffleHog! Build aborted."

                        error "Pipeline stopped: hardcoded secrets found."

                    } else {

                        echo "No secrets detected. Proceeding to build."
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

                    sh '''
                        scp -o StrictHostKeyChecking=no \
                        target/*.war \
                        ubuntu@13.233.1.94:/opt/tomcat/webapps/
                    '''
                }
            }
        }
    }
}
```

```

---

**6.1 Why ‘report.contains("DetectorName")’ and not just ‘file not empty’**

An earlier version of this check tested only whether trufflehog-report.json had content (readFile(...).trim().length() > 0). That is unreliable, because TruffleHog can write informational log lines (level ‘info-0’ status messages) even when zero secrets are found, so the file is never actually empty. Checking specifically for the ‘DetectorName’ field is accurate, because that field is only present in the JSON when an actual secret match has been reported.

**6.2 Deployment steps in Jenkins**

1. Open the Jenkinsfile in the GitHub repository and replace its contents with the pipeline above.
2. Commit the change to the repository.
3. In Jenkins, open the pipeline job and click ‘Build Now’.
4. Open ‘Console Output’ or the Blue Ocean pipeline view to observe stage-by-stage execution.

**7. Proof of Concept: Pipeline Blocks a Build on Detected Secret**

The test commit added a fake AWS access key (AKIAZQ3XJ9K2LMNPQRST, format-matched but not a real credential) to index.jsp, then triggered a Jenkins build. The following two screenshots, taken from the resulting build, confirm the pipeline behaved as designed.

**7.1 Console Output – Detection and Abort**

The console log shows TruffleHog's JSON output for the scan, including the detected AWS key details (SourceType, DetectorName: "AWS", the raw key value, and structured access-key data). Immediately after, the pipeline prints the “SECRETS DETECTED” message, and the Build and Deploy To Tomcat stages are both explicitly marked as “skipped due to earlier failure(s).” The pipeline finishes with status FAILURE.

![](attachment:fb321e06-73a5-44ae-a115-c70b094896d6:f934f1ece5695c3f7910de0fe00653a613cb95d3.png)

*Figure 1 – Jenkins console output: TruffleHog detection (AWS key) followed by pipeline abort and skipped downstream stages.*

**7.2 Blue Ocean View – Pipeline Stage Graph**

The Blue Ocean visual pipeline view shows the same result graphically: ‘Initialize’ completed successfully (green), ‘Secret Scan - TruffleHog’ failed (red), and both ‘Build’ and ‘Deploy To Tomcat’ remained un-run (grey/open circles) because the pipeline halted before reaching them. The step log underneath confirms the ‘SECRETS DETECTED by TruffleHog! Build aborted.’ message and the final error signal ‘Pipeline stopped: hardcoded secrets found in repository.’

![](attachment:e45cd810-6c87-4fac-9e5c-26f841a66aa5:d96d3a53ff8f0aaacf4c4d1216d04ce9840e52ab.png)

*Figure 2 – Blue Ocean pipeline graph: Secret Scan stage failed; Build and Deploy To Tomcat never executed.*

**7.3 Result Summary**

**POC outcome: PASS**

A hardcoded AWS-format credential committed to the repository was detected by the TruffleHog scan stage, the pipeline stopped with a controlled error before compiling or deploying any code, and no artifact built from the vulnerable commit was deployed to the Tomcat server.

---

**8. Operational Notes**

- This check currently flags any detection regardless of verification status (‘Verified’: false is still treated as a blocking finding). This is intentional — unverified does not mean harmless, it only means TruffleHog could not confirm the credential against the live provider API.
- Remove or rotate any real secret immediately if TruffleHog ever reports a ‘Verified’: true detection — that indicates a live, working credential was found in the repository.
- Keep the ‘--no-update’ flag in all Jenkins-triggered scans so pipeline runs are not affected by TruffleHog's self-update mechanism or transient network issues.
- Consider archiving trufflehog-report.json as a Jenkins build artifact for audit history.
- This setup scans the full repository on every run via the GitHub source connector; for large repositories, scope or caching options can be evaluated to reduce scan time.

*End of document.*

# 5-SCA_Snyk_Integration_POC

**CI/CD Security Gate Integration**

**Software Composition Analysis (SCA) using Snyk**

*Project: webapp-cicd-pipeline | Tool: Snyk CLI | Orchestrator: Jenkins Declarative Pipeline*

**1. Objective**

This document records the steps followed to integrate Software Composition Analysis (SCA) into the existing Jenkins CI/CD pipeline using Snyk. SCA scans project dependencies (e.g. Maven libraries) for known vulnerabilities (CVEs) and fails the build if high or critical severity issues are found, before the artifact reaches the Build and Deploy stages.

The pipeline stage order after this integration is:

**Initialize → Secret Scan (TruffleHog) → SCA Scan (Snyk) → SAST Scan (Semgrep) → Build → Deploy To Tomcat**

**2. Prerequisites**

- A Snyk account (free tier is sufficient) at snyk.io
- Jenkins server with administrator access to add credentials
- SSH access to the Jenkins build agent (Ubuntu) to install the Snyk CLI
- A Maven-based project already connected to the pipeline (pom.xml present)

**3. Step 1 — Install the Snyk CLI on the Jenkins Agent**

The Snyk CLI binary was downloaded directly onto the Ubuntu build agent, made executable, and moved into a directory already present on the system PATH so the Jenkinsfile can invoke it.

ubuntu@ip-172-31-8-38:~$ curl -Lo snyk https://static.snyk.io/cli/latest/snyk-linux

% Total % Received % Xferd Average Speed Time Time Time Current

Dload Upload Total Spent Left Speed

100 183.3M 0 183.3M 0 0 83.29M 0 --:--:-- 0:00:02 74.46M

ubuntu@ip-172-31-8-38:~$ chmod +x snyk

ubuntu@ip-172-31-8-38:~$ sudo mv snyk /usr/local/bin/

---

Verification: the CLI is confirmed to be on the PATH and reachable from within the pipeline:

which snyk

snyk --version

---

In the Jenkinsfile, an environment variable SNYK_PATH = "/usr/local/bin" is defined and appended to PATH inside every shell step of the SCA stage, so the pipeline does not depend on the default PATH of the Jenkins agent's shell.

**4. Step 2 — Generate a Snyk API Token**

Snyk authenticates CLI requests using a Personal Access Token (PAT). This token is generated once from the Snyk web console and is what Jenkins will use to run scans non-interactively.

1. Log in to app.snyk.io and open Account Settings.
2. Navigate to Personal Access Tokens.
3. Provide a Name for the token, set an Expiry, and click Generate new token.
4. Copy the generated token immediately — it is only shown once.

![](attachment:1140d646-daf7-4118-ab6d-d772f7d1fa2f:b416b300c701cf5a608933838a31a312eeb7c0ac.png)

*Figure 1 — Generating a Personal Access Token from the Snyk console (Account → Personal Access Tokens).*

**5. Step 3 — Store the Token as a Jenkins Credential**

The token must never be hardcoded in the Jenkinsfile. It is stored in the Jenkins credential store and injected at runtime using withCredentials().

1. In Jenkins, go to Manage Jenkins → Credentials → System → Global credentials (unrestricted).
2. Click Add Credentials.
3. Select Secret text as the credential Kind.

![](attachment:ab5ef1f7-b994-4a0d-8a99-4646e14e3155:01dbc063aab5be628a15d4f38abf74096d87644d.png)

*Figure 2 — Selecting "Secret text" as the credential type in Jenkins.*

1. Paste the Snyk token generated in Step 2 into the Secret field.
2. Set the ID field to exactly: snyk-token (this must match the credentialsId used in the Jenkinsfile — it is case-sensitive).
3. Add a short Description (e.g. "Snyk API Token") and click Create.

![](attachment:39ff1017-f9e8-4c2f-a1c0-fd7067e06eb6:ba78c6baa655f1bc005cb0939a0d289ee4ea5c58.png)

*Figure 3 — Adding the Snyk token as a Secret text credential with ID "snyk-token".*

**6. Step 4 — Add the SCA Stage to the Jenkinsfile**

A new stage, SCA Scan - Snyk, was added to the declarative pipeline. It authenticates using the stored credential, runs a full dependency scan to produce a JSON report for the build artifacts, and then runs a second, gating scan that fails the build if any high or critical severity vulnerability is found.

```jsx
stage('SCA Scan - Snyk') {

    steps {

        withCredentials([
            string(
                credentialsId: 'snyk-token',
                variable: 'SNYK_TOKEN'
            )
        ]) {

            script {

                // Verify Snyk installation
                sh '''
                    export PATH=$PATH:${SNYK_PATH}

                    which snyk
                    snyk --version
                '''

                // Generate complete JSON report
                // Non-blocking: report is generated even if vulnerabilities exist
                sh '''
                    export PATH=$PATH:${SNYK_PATH}

                    snyk test --json > snyk-report.json || true
                '''

                // Display report in Jenkins console
                sh 'cat snyk-report.json'

                // Archive Snyk JSON report
                archiveArtifacts(
                    artifacts: 'snyk-report.json',
                    allowEmptyArchive: true
                )

                // Security gate
                // Fail pipeline if HIGH or CRITICAL vulnerabilities are found
                def snykExitCode = sh(
                    script: '''
                        export PATH=$PATH:${SNYK_PATH}

                        snyk test --severity-threshold=high
                    ''',
                    returnStatus: true
                )

                if (snykExitCode != 0) {

                    error(
                        "Pipeline stopped: high/critical severity vulnerabilities found."
                    )

                } else {

                    echo "No high/critical SCA vulnerabilities found."
                }
            }
        }
    }
}
```

---

Note: SNYK_TOKEN is picked up automatically by the Snyk CLI from the environment — no separate snyk auth step is required once the credential is bound with withCredentials().

**7. Step 5 — Run the Pipeline and Verify**

With the credential in place, the pipeline was re-triggered. The build progressed past Initialize and Secret Scan - TruffleHog into the new SCA Scan - Snyk stage, which produced a live JSON vulnerability report in the console output.

![](attachment:5e36d8e6-3d8e-4896-9c3b-4a1701b0a462:0d9f552f64460a2f388e9187afbc216cce9254e6.png)

*Figure 4 — Jenkins Blue Ocean view: pipeline reaches the SCA Scan - Snyk stage and streams snyk-report.json to the console.*

Findings observed in this run (from snyk-report.json), used as proof that the gate is functioning correctly:

- com.h2database:h2 @ 1.4.197 — 2 Remote Code Execution (RCE) vulnerabilities, one rated CVSS 9.8 (Critical).
- org.apache.logging.log4j:log4j-core @ 2.14.1 — CVE-2021-44228 ("Log4Shell"), a critical Remote Code Execution vulnerability.

Because the stage is configured with --severity-threshold=high, the build correctly failed (FAILURE) at this stage instead of proceeding to Build and Deploy — this is the intended behaviour of a DevSecOps quality gate, not a pipeline defect.

**8. Remediation Applied**

1. Upgrade com.h2database:h2 in pom.xml from 1.4.197 to 2.1.210 or later.
2. Upgrade org.apache.logging.log4j:log4j-core in pom.xml from 2.14.1 to 2.25.5 or later (resolves Log4Shell and related CVEs).
3. Run mvn clean package locally to confirm the project still builds against the updated dependency versions.
4. Commit and push the changes so the pipeline re-runs; the SCA Scan - Snyk stage is expected to pass once vulnerable versions are removed.

**9. Summary**

The Snyk CLI was installed on the Jenkins agent, authenticated using a Personal Access Token stored as a Jenkins "Secret text" credential (ID: snyk-token), and wired into the pipeline as a dedicated SCA Scan - Snyk stage placed after the Secret Scan and before the SAST Scan. The stage archives a full JSON report as a build artifact and fails the pipeline on any high or critical severity dependency vulnerability, giving the pipeline three independent security gates — secrets, dependencies (SCA), and source code (SAST) — before an artifact can be built and deployed.
# 6-sast

Try AI directly in your favorite apps … Use Gemini to generate drafts and refine content, plus get Gemini Pro with access to Google’s next-gen AI for ₹1,950 ₹489 for 3 months
100%

## sast

```
──(shubham㉿kali)-[~/webapp]
└─$ pipx install semgrep

  installed package semgrep 1.177.0, installed using Python 3.13.9
  These apps are now available
    - pysemgrep
    - semgrep
done! ✨ 🌟 ✨

┌──(shubham㉿kali)-[~/webapp]
└─$ semgrep --version
1.177.0

# ubuntu user ke home aur .local folder tak jenkins ko path access do
sudo chmod o+x /home/ubuntu
sudo chmod -R o+rX /home/ubuntu/.local

# verify — jenkins user se test karo
sudo -u jenkins /home/ubuntu/.local/bin/semgrep --version

```````````````````````````````````````````````````````````````````````````````````````````````````````
┌──(shubham㉿kali)-[~/webapp]
└─$ semgrep scan --config auto --verbose .

┌──(shubham㉿kali)-[~/webapp]
└─$ semgrep scan --config p/owasp-top-ten --config p/secrets --config p/security-audit .

````````````````````````````````````````````Jenkins`````````````````````````````````````````````````````````````````````
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
                    sh 'scp -o StrictHostKeyChecking=no target/*.war ubuntu@65.2.73.79:/opt/tomcat/webapps/'
                }
            }
        }
    }
}
```

end!

# 7-DAST_Integration_Guide

**DAST Integration Guide**

Integrating OWASP ZAP into a Jenkins CI/CD Pipeline

*Project: webapp-cicd-pipeline*

1. Overview

This document describes how Dynamic Application Security Testing (DAST) was added to an existing Jenkins CI/CD pipeline, using OWASP ZAP (Zed Attack Proxy). Unlike SAST (Semgrep) and SCA (Snyk), which analyze source code and dependencies before the application is built, DAST scans the application while it is actually running — after deployment — by sending real HTTP requests to it and observing the responses.

The complete pipeline flow, in order, is:

1. Secret Scan (TruffleHog) – scans the Git repository for hardcoded secrets.
2. SCA Scan (Snyk) – scans project dependencies (pom.xml) for known vulnerable libraries.
3. SAST Scan (Semgrep) – scans the Java source code for insecure coding patterns.
4. Build (Maven) – compiles the application and packages it as a WAR file.
5. Deploy to Tomcat – copies the WAR file to the Tomcat application server.
6. DAST Scan (OWASP ZAP) – scans the live, deployed application for runtime vulnerabilities.

A key design decision for this pipeline is that none of the security stages block the build. Every scan stage marks the Jenkins build as UNSTABLE (not FAILURE) when issues are found, and the pipeline always continues through Build, Deploy, and DAST. This was intentional, to allow the team to see and track all findings across every layer of security testing in a single build, rather than stopping at the first failure.

2. Environment / Architecture

The setup uses two separate AWS EC2 instances:

- Jenkins server — runs the pipeline, Semgrep, Snyk, TruffleHog, and OWASP ZAP.
- Tomcat server — hosts the deployed web application (WebApp.war).

Because Jenkins and Tomcat are two different EC2 instances, OWASP ZAP (running on the Jenkins server) has to reach the Tomcat server over the network using its address — not "localhost". This distinction turned out to be the main source of connectivity issues during setup, covered in Section 4.

![](attachment:ff842211-957f-4fa3-8d2d-3c41b99324c9:6b77b80da2699dab1f757a1c6503ca1d9a8b0b64.png)

*Figure 1 — The two EC2 instances used in this setup: "Tomcat" (application server) and "Jenkins" (CI/CD server), each with its own public and private IP address.*

3. Installing OWASP ZAP on the Jenkins Server

OWASP ZAP was installed directly on the Jenkins EC2 instance as a standalone application (not via Docker), since ZAP needs to be invoked as a shell command from within the Jenkins pipeline.

3.1 Prerequisites

ZAP requires Java. The Jenkins server already had OpenJDK 21 installed, which was verified with:

java -version

3.2 Download and Install

The ZAP Linux installer was downloaded from the official GitHub releases page and installed in quiet (unattended) mode:

wget https://github.com/zaproxy/zaproxy/releases/download/v2.16.1/ZAP_2_16_1_unix.sh

chmod +x ZAP_2_16_1_unix.sh

sudo ./ZAP_2_16_1_unix.sh -q

This installs ZAP to the default location:

/opt/zaproxy

ZAP can then be run from the command line using the zap.sh script located at /opt/zaproxy/zap.sh, which is exactly what the Jenkins pipeline calls in the DAST stage (Section 5).

4. Networking Requirements — Security Group Configuration

This was the most important — and most time-consuming — part of the DAST setup. Because Jenkins and Tomcat live on two separate EC2 instances, the Jenkins server must be able to reach the Tomcat server's application port over the network. By default, AWS Security Groups block all inbound traffic that is not explicitly allowed, which caused ZAP to repeatedly fail with a connection timeout when it tried to scan the deployed application.

4.1 Rule required: Tomcat instance (inbound)

On the Security Group attached to the Tomcat EC2 instance, an inbound rule was added to allow traffic from the Jenkins server on the Tomcat application port:

| **Type** | **Protocol** | **Port** | **Source** | **Purpose** |
| --- | --- | --- | --- | --- |
| Custom TCP | TCP | 8080 | 172.31.8.38/32 (Jenkins private IP) | Allows Jenkins / ZAP to reach the deployed WebApp |
| SSH | TCP | 22 | 0.0.0.0/0 (or admin IP only) | Allows SSH access for deployment / troubleshooting |

4.2 Rule required: Jenkins instance (outbound)

On the Security Group attached to the Jenkins EC2 instance, the default outbound rule (present on new AWS security groups) was kept as-is, since it already permits all outbound traffic:

| **Type** | **Protocol** | **Port** | **Destination** | **Purpose** |
| --- | --- | --- | --- | --- |
| All traffic | All | All | 0.0.0.0/0 | Default outbound rule — allows Jenkins/ZAP to initiate connections to Tomcat and the internet |

4.3 Important prerequisite: same VPC

For the private-IP based rule above to work at all, both EC2 instances must be in the same VPC (and ideally the same subnet / availability zone). Private IP addresses are only directly routable within the same VPC; a private IP from one VPC cannot be reached from an instance in a different VPC without VPC peering or a similar setup. Both instances in this project were confirmed to be in the same VPC before the rule was added.

![](attachment:bb0490bc-5cd7-4c31-aa0f-aab5b7e8efad:f4692929dc2e3963d24bb71d053f717073410380.png)

*Figure 2 — Inbound rules on the Tomcat instance's Security Group: SSH (22) open, and Custom TCP (8080) restricted to the Jenkins server's private IP (172.31.8.38/32).*

4.4 Symptom before the fix

Before this rule was added, running the DAST stage produced the following in the ZAP output, even though the same URL worked perfectly when opened from the Tomcat server itself (localhost):

Failed to attack the URL: Connect to http://13.204.66.212:8080 [/13.204.66.212] failed: Connect timed out

This is the classic signature of a Security Group block: the TCP connection attempt is silently dropped rather than actively refused, so the client waits for the full timeout period before giving up.

5. Jenkinsfile Integration — DAST Stage

The DAST stage was added as the final stage of the pipeline, placed immediately after "Deploy To Tomcat", since ZAP needs the application to already be running before it can scan it.

5.1 Pipeline environment variables

```
pipeline {
    agent any
    environment {
        SEMGREP_PATH = "/home/ubuntu/.local/bin"
        SNYK_PATH    = "/usr/local/bin"
        ZAP_PATH     = "/opt/zaproxy"
        DEPLOY_HOST  = "13.204.66.212"
        APP_URL      = "http://13.204.66.212:8080/WebApp/"
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
```

5.3 What each part does

- sleep 25 — gives Tomcat time to fully load the newly deployed WAR file before it is scanned.
- pkill -f zap.sh || true — kills any ZAP process left running from a previous (possibly failed) build, so it does not hold onto a port.
- port 8090 — tells ZAP to run its internal proxy on port 8090 instead of its default port 8080. This was required because Jenkins itself runs on port 8080 on the same server, and ZAP's default port clashed with it, causing a java.net.BindException: Address already in use error.
- quickurl ${APP_URL} — runs ZAP's Quick Scan (spider + active scan) against the deployed application URL.
- quickout — writes the scan results to an HTML report (zap-report.html) inside the Jenkins workspace.
- archiveArtifacts — makes the report downloadable from the Jenkins build page (Artifacts tab).
- The High-risk count check — a lightweight way of flagging serious findings in the Jenkins console without failing the build; the report itself contains the full detail.

As with the other scan stages (Secret Scan, SCA, SAST), this stage never fails the build outright — it only marks it UNSTABLE — so that Build and Deploy always complete and the DAST report is always produced for review.

6. Issues Encountered and Fixes

A short summary of the real issues hit while wiring this up, for future reference:

| **Symptom** | **Root Cause** | **Fix** |
| --- | --- | --- |
| ZAP failed to start: java.net.BindException: Address already in use | ZAP's default proxy port (8080) is the same port Jenkins itself runs on, on the same server | Explicitly set ZAP to use a different port with -port 8090 |
| ZAP: "Failed to attack the URL ... Connect timed out" | AWS Security Group on the Tomcat instance did not allow inbound traffic on port 8080 from the Jenkins server's IP | Added an inbound rule for port 8080, source = Jenkins private IP |
| curl from the Jenkins server to the Tomcat public IP also timed out | Same Security Group restriction — confirmed the block was independent of any local SSH tunnel / browser setup | Same fix as above; verified with curl -I from inside the Jenkins server after the rule was added |
| Jenkins UI itself became unreachable at one point | The SSH command used to reach the Jenkins server was missing the -L 8080:localhost:8080 port-forwarding flag | Reconnected with the correct SSH command including -L 8080:localhost:8080 |

7. Pipeline Execution Result

Once the port conflict and the Security Group rule were both fixed, the DAST stage completed a full ZAP Quick Scan (spider + active scan, 0% to 100%) against the live application and produced a report — all without failing the pipeline.

![](attachment:b7cca014-6fbe-4843-b83d-962bd97ab7ee:99438d7aa235ae95795d17b1f1e991190b0f33e7.png)

*Figure 3 — Jenkins Blue Ocean view of the completed pipeline. All stages, including "DAST Scan - OWASP ZAP", finished successfully; the build overall was marked UNSTABLE because earlier scan stages (SCA, SAST) found issues — by design, this does not block Build or Deploy.*

8. ZAP DAST Scan Report Summary

The generated zap-report.html is attached to every Jenkins build under the Artifacts tab, and can also be opened directly at a URL of the form:

http://localhost:8080/job/webapp-cicd-pipeline/<build-number>/artifact/zap-report.html

Summary of alerts from the scan of the deployed application:

![](attachment:9c589093-786d-42f6-89da-af6a4b8601ce:6afef3836f7b48b918b366c57414b4d09ad0c118.png)

*Figure 4 — ZAP alert summary table from the HTML report, grouped by risk level and number of instances.*

| **Risk Level** | **Number of Alert Types** | **Examples** |
| --- | --- | --- |
| High | 7 | SQL Injection, XSS (Reflected & Persistent), Path Traversal, Open/External Redirect, Remote File Inclusion |
| Medium | 4 | Missing CSP header, Missing anti-clickjacking header, Absence of Anti-CSRF tokens, XSLT Injection |
| Low | 3 | Missing X-Content-Type-Options header, Cookie without SameSite attribute, Cross-domain JS inclusion |
| Informational | 6 | Suspicious code comments, session token identified, modern web app detected |

Several High-risk findings from ZAP directly correspond to issues already flagged earlier in the pipeline by the SAST tool (Semgrep) — for example, the SQL Injection alerts on /login, /search, and /products match Semgrep's formatted-sql-string and tainted-sql-from-http-request findings in LoginServlet.java, SearchServlet.java, and ProductServlet.java. This overlap is expected and useful: SAST identifies the vulnerable pattern in the source code, while DAST confirms that the vulnerability is actually exploitable in the running application.

9. Conclusion and Next Steps

OWASP ZAP is now fully integrated into the CI/CD pipeline as an automated DAST stage that runs on every build, after deployment, without blocking the pipeline. Recommended next steps:

- Fix the High-risk findings (SQL Injection, XSS, Path Traversal, Open Redirect) identified in the current scan.
- Move from ZAP's Quick Scan to an authenticated full scan (zap-full-scan.py with a login context) to test pages that require a logged-in session.
- Introduce severity-based gating once the current known issues are triaged — for example, fail the build only on newly introduced Critical/High findings, while continuing to allow known, tracked issues through as UNSTABLE.
- Restrict the Security Group rule to the narrowest possible scope (already done here, limited to the Jenkins private IP) and review it periodically.
