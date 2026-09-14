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
 * VULN-1: SQL Injection - Error-based (CWE-89)
 *
 * The 'q' parameter is concatenated directly into the SQL string. If the
 * query is malformed by injected input, the raw JDBC/H2 exception message
 * is written straight to the HTTP response - this is what makes it
 * "error-based": the attacker doesn't need blind inference, the database
 * itself hands over schema/version details in the error text.
 *
 * Forwards results (and the raw 'q' value) to search.jsp, which also
 * contains VULN-5 (Reflected XSS) - see that file for details.
 */
@WebServlet("/search")
public class SearchServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String q = req.getParameter("q");
        if (q == null) q = "";

        StringBuilder results = new StringBuilder();

        // VULN-1: string concatenation into SQL, no PreparedStatement.
        String query = "SELECT name, description, price FROM products WHERE name LIKE '%" + q + "%'";

        try (Connection conn = DriverManager.getConnection(
                DBInitListener.JDBC_URL, DBInitListener.DB_USER, DBInitListener.DB_PASS);
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(query)) {

            while (rs.next()) {
                results.append("<li>").append(rs.getString("name"))
                        .append(" - $").append(rs.getString("price")).append("</li>");
            }
        } catch (Exception e) {
            // VULN-1 (the actual error-based signature): raw DB exception
            // text is exposed to the client instead of a generic error page.
            req.setAttribute("dbError", e.getMessage());
        }

        req.setAttribute("query", q);
        req.setAttribute("results", results.toString());
        req.getRequestDispatcher("/search.jsp").forward(req, resp);
    }
}
