package com.webapp.vulnlab;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * VULN-11: SSRF - Server-Side Request Forgery (CWE-918, OWASP A10:2021)
 *
 * "Website status checker" - the server fetches whatever URL the client
 * supplies, with no allowlist of hosts/schemes and no block on private/
 * link-local IP ranges. The server (not the browser) makes the request,
 * so it can reach things the client never could directly: internal admin
 * panels, other services on localhost, or - critically on real cloud
 * infrastructure - the instance metadata service.
 *
 * *** THIS IS THE MOST DANGEROUS LAB PAGE IN THIS APP. ***
 * If this app is ever deployed on a real AWS EC2 instance with IMDSv1
 * enabled and an attached IAM role, an attacker can request:
 *   ?url=http://169.254.169.254/latest/meta-data/iam/security-credentials/&lt;role-name&gt;
 * and receive temporary AWS access keys for that instance's IAM role -
 * a real, well-documented cloud takeover technique (see Capital One 2019
 * breach). Enforce IMDSv2 and lock down security groups before exposing
 * this lab publicly - see VULNERABILITIES.md.
 *
 * Example (safe) demo payloads:
 *   ?url=http://localhost:8080/WebApp/admin      (reach an internal-only page)
 *   ?url=http://169.254.169.254/latest/meta-data/  (cloud metadata - EC2 only)
 */
@WebServlet("/url-preview")
public class UrlPreviewServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String targetUrl = req.getParameter("url");
        resp.setContentType("text/html");

        if (targetUrl == null || targetUrl.isEmpty()) {
            resp.getWriter().println("<h3>Missing url parameter.</h3>");
            return;
        }

        // VULN-11: no host allowlist, no scheme restriction, no block on
        // RFC 1918 / link-local (169.254.0.0/16) destination addresses.
        HttpURLConnection conn = null;
        try {
            URL url = new URL(targetUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);
            conn.setRequestMethod("GET");

            int status = conn.getResponseCode();
            StringBuilder body = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()))) {
                String line;
                int maxLines = 50; // trimmed for display only
                while ((line = reader.readLine()) != null && maxLines-- > 0) {
                    body.append(line).append("\n");
                }
            }

            resp.getWriter().println("<h3>Status: " + status + "</h3><pre>"
                    + body + "</pre>");
        } catch (Exception e) {
            resp.getWriter().println("<h3>Fetch error: " + e.getMessage() + "</h3>");
        } finally {
            if (conn != null) conn.disconnect();
        }
    }
}
