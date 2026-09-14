package com.webapp.vulnlab;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * VULN-9: Broken Access Control - Missing Function-Level Authorization
 * (CWE-862, OWASP A01:2021)
 *
 * This "admin only" page never checks req.getSession().getAttribute("role").
 * It is reachable by directly typing the URL - no login required at all.
 * Dumps every user's username, role, and SSN.
 *
 * Compare with ProfileServlet (VULN-8): that one at least requires knowing/
 * guessing an id one at a time. This one dumps everything in a single request.
 */
@WebServlet("/admin")
public class AdminServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // VULN-9: no authentication or role check whatsoever before serving
        // this page. A correct implementation would check:
        //   Object role = req.getSession(false) != null
        //       ? req.getSession(false).getAttribute("role") : null;
        //   if (!"admin".equals(role)) { resp.sendError(403); return; }

        resp.setContentType("text/html");
        StringBuilder rows = new StringBuilder();

        try (Connection conn = DriverManager.getConnection(
                DBInitListener.JDBC_URL, DBInitListener.DB_USER, DBInitListener.DB_PASS);
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT id, username, role, ssn FROM users")) {

            while (rs.next()) {
                rows.append("<tr><td>").append(rs.getInt("id"))
                        .append("</td><td>").append(rs.getString("username"))
                        .append("</td><td>").append(rs.getString("role"))
                        .append("</td><td>").append(rs.getString("ssn"))
                        .append("</td></tr>");
            }

            resp.getWriter().println(
                    "<h2>Admin Panel - All Users</h2>"
                            + "<table border='1'><tr><th>ID</th><th>Username</th>"
                            + "<th>Role</th><th>SSN</th></tr>" + rows + "</table>");
        } catch (Exception e) {
            resp.getWriter().println("<h3>Error: " + e.getMessage() + "</h3>");
        }
    }
}
