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
 * VULN-2: SQL Injection - Blind Boolean-based (CWE-89)
 *
 * Unlike SearchServlet (error-based), this endpoint swallows every
 * exception and only ever shows one of two generic messages:
 * "Product exists" or "Product not found". No data, no error text is
 * ever leaked. An attacker must infer the true/false result of each
 * injected boolean condition one bit at a time (e.g. via SUBSTRING()
 * guesses) to extract data - hence "blind".
 *
 * Example payloads (see VULNERABILITIES.md for full walkthrough):
 *   ?id=1 AND 1=1   -> "Product exists"
 *   ?id=1 AND 1=2   -> "Product not found"
 */
@WebServlet("/search-blind")
public class SearchBlindServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String id = req.getParameter("id");
        if (id == null) id = "1";

        boolean found = false;

        // VULN-2: concatenated into SQL; errors are deliberately hidden below,
        // forcing an attacker to rely purely on the true/false response.
        String query = "SELECT id FROM products WHERE id = " + id;

        try (Connection conn = DriverManager.getConnection(
                DBInitListener.JDBC_URL, DBInitListener.DB_USER, DBInitListener.DB_PASS);
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(query)) {

            found = rs.next();
        } catch (Exception e) {
            // Intentionally swallowed - no error is ever shown to the client.
            // This is what makes exploitation "blind" rather than "error-based".
            found = false;
        }

        resp.setContentType("text/html");
        if (found) {
            resp.getWriter().println("<h3>Product exists.</h3>");
        } else {
            resp.getWriter().println("<h3>Product not found.</h3>");
        }
    }
}
