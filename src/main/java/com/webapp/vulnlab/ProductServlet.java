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
 * VULN-3: SQL Injection - Union-based (CWE-89)
 *
 * Returns exactly 2 columns (name, price) directly into an HTML table.
 * Because the column count and rough types are guessable/enumerable via
 * ORDER BY probing, an attacker can append a UNION SELECT with a matching
 * column count to pull data out of a completely different table (e.g. the
 * users table) through this same response.
 *
 * Example exploit (see VULNERABILITIES.md):
 *   ?category=nonexistent' UNION SELECT username, password FROM users --
 */
@WebServlet("/products")
public class ProductServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String category = req.getParameter("category");
        if (category == null) category = "electronics";

        StringBuilder rows = new StringBuilder();

        // VULN-3: concatenated into SQL; exactly 2 selected columns makes a
        // UNION-based attack straightforward once the attacker enumerates
        // the column count (e.g. via "ORDER BY 3" causing an error).
        String query = "SELECT name, price FROM products WHERE category = '" + category + "'";

        resp.setContentType("text/html");

        try (Connection conn = DriverManager.getConnection(
                DBInitListener.JDBC_URL, DBInitListener.DB_USER, DBInitListener.DB_PASS);
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(query)) {

            while (rs.next()) {
                rows.append("<tr><td>").append(rs.getString(1))
                        .append("</td><td>").append(rs.getString(2))
                        .append("</td></tr>");
            }
            resp.getWriter().println(
                    "<table border='1'><tr><th>Name</th><th>Price</th></tr>"
                            + rows + "</table>");
        } catch (Exception e) {
            resp.getWriter().println("<h3>Query error: " + e.getMessage() + "</h3>");
        }
    }
}
