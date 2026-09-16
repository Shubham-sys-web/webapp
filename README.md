# DevSecOps Web Application CI/CD Pipeline

> **End-to-end CI/CD + DevSecOps pipeline on AWS EC2 — automated build, security scanning, deployment, and runtime testing.**

This project demonstrates a practical DevSecOps pipeline for a Java web application using **GitHub, Jenkins, Maven, TruffleHog, Snyk, Semgrep, Apache Tomcat, and OWASP ZAP**.

The pipeline brings security checks into the delivery workflow instead of treating security as a separate activity.

---

## 🚀 Project Highlights

- Automated CI/CD with **Jenkins Pipeline as Code**
- GitHub integration using **Webhook / Poll SCM**
- Maven-based application build and WAR packaging
- **Secret scanning** with TruffleHog
- **Software Composition Analysis (SCA)** with Snyk
- **Static Application Security Testing (SAST)** with Semgrep
- Automated deployment to **Apache Tomcat** over SSH
- **Dynamic Application Security Testing (DAST)** with OWASP ZAP
- Security reports archived as Jenkins build artifacts
- AWS EC2-based Jenkins and Tomcat environment

---

## 🏗️ Architecture

The project uses two AWS EC2 instances:

1. **Jenkins Server** — CI/CD orchestration and security tooling
2. **Tomcat Server** — hosts the deployed Java web application

### Architecture Diagram

![DevSecOps Architecture](https://github.com/Shubham-sys-web/webapp/blob/master/images/architecture.png)

---

## 🔄 Pipeline Flow

```text
GitHub
   │
   ▼
Jenkins
   │
   ├── Initialize
   │
   ├── Secret Scan ─────── TruffleHog
   │
   ├── SCA Scan ────────── Snyk
   │
   ├── SAST Scan ───────── Semgrep
   │
   ├── Build ───────────── Maven
   │
   ├── Deploy ──────────── SSH + SCP
   │                         │
   │                         ▼
   │                    Apache Tomcat
   │                         │
   │                         ▼
   └── DAST Scan ───────── OWASP ZAP
```

### Pipeline Stages

`Initialize → TruffleHog → Snyk → Semgrep → Maven Build → Tomcat Deployment → OWASP ZAP`

Each security layer checks a different part of the application:

| Layer | Tool | Purpose |
|---|---|---|
| Secret Security | TruffleHog | Detect hardcoded secrets and credentials |
| Dependency Security | Snyk | Identify vulnerable Maven dependencies / CVEs |
| Source Security | Semgrep | Detect insecure coding patterns |
| Build | Maven | Compile, test and package the application |
| Deployment | Jenkins + SSH/SCP + Tomcat | Automatically deploy the WAR |
| Runtime Security | OWASP ZAP | Test the running application for web vulnerabilities |

---

## 🛠️ Technology Stack

**Source Control**
- Git
- GitHub

**CI/CD**
- Jenkins
- Jenkins Pipeline / Jenkinsfile
- Maven

**Security**
- TruffleHog — Secret Scanning
- Snyk — SCA
- Semgrep — SAST
- OWASP ZAP — DAST

**Application Server**
- Apache Tomcat 9

**Infrastructure**
- AWS EC2
- SSH
- Linux / Ubuntu

---

## 🔐 Security Integration

### 1. Secret Scanning — TruffleHog

TruffleHog scans the repository for accidentally committed credentials such as API keys, access keys, and private keys.

A proof of concept was performed using an AWS-format test credential. When a secret was detected, the pipeline stopped before Build and Deploy.

**Security benefit:** prevents credentials from moving further through the delivery pipeline.

---

### 2. Software Composition Analysis — Snyk

Snyk scans Maven dependencies from `pom.xml` for known vulnerabilities and CVEs.

The POC identified vulnerable dependencies including older H2 and Log4j versions. The pipeline was configured to use a severity threshold for High/Critical findings.

**Security benefit:** identifies vulnerable third-party libraries before release.

---

### 3. Static Application Security Testing — Semgrep

Semgrep analyzes Java source code using security-focused rulesets including:

- OWASP Top 10
- Secrets
- Security audit rules

The pipeline generates `semgrep-report.json` and reports findings with file, line and severity information.

**Security benefit:** finds insecure coding patterns before the application is deployed.

---

### 4. Automated Deployment — Apache Tomcat

After the application is built successfully, Jenkins transfers the generated WAR file to the Tomcat server using SSH/SCP.

Tomcat then auto-deploys the WAR from its `webapps` directory.

**Security note:** the POC used relaxed directory permissions for demonstration. A production setup should use least-privilege ownership/groups instead of `777`.

---

### 5. Dynamic Application Security Testing — OWASP ZAP

OWASP ZAP runs after deployment and tests the live application using HTTP requests.

The DAST POC identified multiple classes of web security issues, including examples such as:

- SQL Injection
- Cross-Site Scripting (XSS)
- Path Traversal
- Open Redirect
- Security-header issues
- CSRF-related findings

The generated `zap-report.html` is archived by Jenkins for review.

**Security benefit:** validates security behavior in the running application and complements SAST findings.

---

## 📦 Prerequisites

- JDK 21 on Jenkins server
- JDK 17 on Tomcat server
- Git and GitHub repository
- Jenkins
- Maven
- TruffleHog
- Snyk CLI + Snyk account
- Semgrep
- Apache Tomcat 9
- OWASP ZAP
- Two AWS EC2 instances for Jenkins and Tomcat
- Network connectivity between the EC2 instances

---

## 📚 Project Documentation

Detailed setup and POC documentation is available in the repository.

### 1. Jenkins + Tomcat Setup

Complete environment setup for Jenkins and Apache Tomcat.

[📄 Jenkins & Tomcat Setup Guide](https://github.com/Shubham-sys-web/webapp/blob/master/PROJECT-DEVELOPEMENT/1-Jenkins_Tomcat_Setup_Guide%20.pdf)

### 2. Continuous Integration — Jenkins

Covers Jenkins Pipeline creation, GitHub SCM integration, webhooks/Poll SCM, Jenkinsfile configuration, Maven build execution, troubleshooting, and successful CI validation.

[📄 Continuous Integration Jenkins POC](https://github.com/Shubham-sys-web/webapp/blob/master/PROJECT-DEVELOPEMENT/2-Continuous_Integration_Jenkins_POC.docx.pdf)

### 3. Continuous Deployment — Tomcat

Covers SSH credentials, Tomcat deployment configuration, WAR transfer using SCP, Jenkins deployment stages, and end-to-end deployment verification.

[📄 Continuous Deployment Tomcat POC](https://github.com/Shubham-sys-web/webapp/blob/master/PROJECT-DEVELOPEMENT/3-Continuous_Deployment_Tomcat_POC.docx.pdf)

### 4. Secret Scanning — TruffleHog

Covers TruffleHog installation, Jenkins integration, detection logic, test-secret POC, pipeline blocking behavior, and operational notes.

[📄 TruffleHog Jenkins Secret Scanning](https://github.com/Shubham-sys-web/webapp/blob/master/PROJECT-DEVELOPEMENT/4-TruffleHog_Jenkins_Secret_Scanning_Docs.docx.pdf)

### 5. SCA — Snyk

Covers Snyk CLI setup, API token configuration, Jenkins credentials, dependency scanning, JSON reporting, severity gating, and remediation.

[📄 Snyk SCA Integration POC](https://github.com/Shubham-sys-web/webapp/blob/master/PROJECT-DEVELOPEMENT/5-SCA_Snyk_Integration_POC.docx.pdf)

### 6. SAST — Semgrep

Covers Semgrep installation, Jenkins integration, OWASP/security rulesets, JSON reporting, vulnerability detection, and pipeline handling.

[📄 SAST — Semgrep](https://github.com/Shubham-sys-web/webapp/blob/master/PROJECT-DEVELOPEMENT/6-sast.txt)

### 7. DAST — OWASP ZAP

Covers ZAP installation, AWS networking/security-group configuration, Jenkins integration, runtime scanning, HTML reporting, and troubleshooting.

[📄 DAST Integration Guide](https://github.com/Shubham-sys-web/webapp/blob/master/PROJECT-DEVELOPEMENT/7-DAST_Integration_Guide.docx.pdf)

---

## 🧪 Proof of Concept Results

The project includes practical validation of the security pipeline:

- **TruffleHog:** detected a format-matching AWS test secret and stopped downstream Build/Deploy stages.
- **Snyk:** identified vulnerable Maven dependencies, including critical Log4j/H2 findings in the tested dependency set.
- **Semgrep:** generated structured SAST findings with file, line, severity and message details.
- **Tomcat:** successfully received and auto-deployed the generated WAR artifact.
- **OWASP ZAP:** scanned the live application and produced an HTML security report.

The project therefore demonstrates security testing across **secrets, dependencies, source code, and the running application**.

---

## 🐛 Troubleshooting Lessons

A few real setup issues were diagnosed during the POCs:

- `jenkinsfile` vs `Jenkinsfile` filename case mismatch
- Jenkins Git/tool configuration
- Java/tool version compatibility
- Disk-space limitations on a small EC2 instance
- TruffleHog cached executable path
- TruffleHog auto-update permission behavior
- Tomcat deployment permissions
- AWS Security Group connectivity between Jenkins and Tomcat
- ZAP port conflict with Jenkins
- Tomcat startup time before DAST execution

These issues were diagnosed through Jenkins console logs, tool output, and infrastructure configuration.

---

## 📈 DevSecOps Value

This project demonstrates a shift-left security workflow:

```text
        CODE
         │
         ▼
   ┌─────────────┐
   │ TruffleHog  │  ← Secrets
   └──────┬──────┘
          ▼
   ┌─────────────┐
   │    Snyk     │  ← Dependencies
   └──────┬──────┘
          ▼
   ┌─────────────┐
   │   Semgrep   │  ← Source Code
   └──────┬──────┘
          ▼
   ┌─────────────┐
   │    Maven    │  ← Build
   └──────┬──────┘
          ▼
   ┌─────────────┐
   │   Tomcat    │  ← Deployment
   └──────┬──────┘
          ▼
   ┌─────────────┐
   │  OWASP ZAP  │  ← Runtime
   └─────────────┘
```

Instead of relying on a single security scanner, the pipeline combines multiple testing layers so different classes of security issues can be identified throughout the software delivery lifecycle.

---

## 🎯 Future Improvements

Planned improvements based on the POCs include:

- Use authenticated OWASP ZAP scans for protected application areas
- Introduce severity-based DAST gates for newly introduced vulnerabilities
- Replace broad filesystem permissions with least-privilege deployment permissions
- Improve credential and secret management
- Add richer security-report visualization
- Add automated remediation workflows
- Expand CI/CD monitoring and notifications

---

## 👨‍💻 Project Goal

The goal of this project is to demonstrate a practical **DevSecOps CI/CD implementation** where application delivery and security testing are integrated into the same automated workflow.

```text
GitHub
  ↓
Jenkins
  ↓
Security Scans
  ↓
Maven Build
  ↓
Tomcat Deployment
  ↓
OWASP ZAP
  ↓
Security Reports
```

**Build → Scan → Deploy → Test → Improve**

---

## 📌 Repository

**GitHub:**  
https://github.com/Shubham-sys-web/webapp
