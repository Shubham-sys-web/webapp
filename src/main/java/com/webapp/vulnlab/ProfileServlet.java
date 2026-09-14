package com.webapp.vulnlab;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * VULN-8: IDOR / Broken Object-Level Authorization (CWE-639, OWASP API1:2023)
 *
 * Note this query IS parameterized (no SQLi here) - the bug is purely
 * authorization: the servlet never checks whether the logged-in session
 * user actually owns the requested id. Any logged-in user (or in this
 * lab, even a guest) can view any other user's record - including the
 * SSN field - just by changing the number in the URL.
 *
 * Example: /profile?id=1  vs  /profile?id=2  vs  /profile?id=3
 */
@WebServlet("/profile")
public class ProfileServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String idParam = req.getParameter("id");
        if (idParam == null) idParam = "1";

        resp.setContentType("text/html");

        // VULN-8: no check that req.getSession().getAttribute("username")
        // corresponds to this id - the query is safe, the authorization is not.
        String query = "SELECT id, username, role, ssn FROM users WHERE id = ?";

        try (Connection conn = DriverManager.getConnection(
                DBInitListener.JDBC_URL, DBInitListener.DB_USER, DBInitListener.DB_PASS);
             PreparedStatement ps = conn.prepareStatement(query)) {

            ps.setInt(1, Integer.parseInt(idParam));

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    resp.getWriter().println(
                            "<h3>Profile</h3>"
                                    + "<p>Username: " + rs.getString("username") + "</p>"
                                    + "<p>Role: " + rs.getString("role") + "</p>"
                                    + "<p>SSN: " + rs.getString("ssn") + "</p>");
                } else {
                    resp.getWriter().println("<h3>No such user.</h3>");
                }
            }
        } catch (NumberFormatException nfe) {
            resp.getWriter().println("<h3>Invalid id.</h3>");
        } catch (Exception e) {
            resp.getWriter().println("<h3>Error: " + e.getMessage() + "</h3>");
        }
    }
}
