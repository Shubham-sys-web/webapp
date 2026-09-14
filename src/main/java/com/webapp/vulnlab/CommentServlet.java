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
import java.util.ArrayList;
import java.util.List;

/**
 * VULN-6: Stored XSS (CWE-79)
 *
 * The INSERT here is parameterized (no SQLi) - the bug is entirely on the
 * output side, in comment.jsp, where every comment's raw message is written
 * into the page with <%= c[1] %> instead of an HTML-escaping helper. Any
 * HTML/JS submitted here is saved to the DB and then executes in the
 * browser of every single visitor who later views the guestbook - that
 * persistence is what makes it "stored" rather than "reflected".
 *
 * Example payload: <script>alert(document.cookie)</script>
 */
@WebServlet("/comment")
public class CommentServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String author = req.getParameter("author");
        String message = req.getParameter("message");
        if (author == null) author = "anonymous";
        if (message == null) message = "";

        String insert = "INSERT INTO comments (author, message) VALUES (?, ?)";

        try (Connection conn = DriverManager.getConnection(
                DBInitListener.JDBC_URL, DBInitListener.DB_USER, DBInitListener.DB_PASS);
             PreparedStatement ps = conn.prepareStatement(insert)) {

            ps.setString(1, author);
            ps.setString(2, message); // stored as-is, no sanitization - the XSS
                                       // is triggered later, on read, in comment.jsp
            ps.executeUpdate();
        } catch (Exception e) {
            resp.getWriter().println("<h3>Error saving comment: " + e.getMessage() + "</h3>");
            return;
        }

        resp.sendRedirect("comment.jsp");
    }

    /** Used by comment.jsp to fetch all comments for display. */
    public static List<String[]> getAllComments() {
        List<String[]> list = new ArrayList<>();
        String query = "SELECT author, message FROM comments ORDER BY id DESC";

        try (Connection conn = DriverManager.getConnection(
                DBInitListener.JDBC_URL, DBInitListener.DB_USER, DBInitListener.DB_PASS);
             PreparedStatement ps = conn.prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(new String[]{rs.getString("author"), rs.getString("message")});
            }
        } catch (Exception e) {
            // best-effort demo code - swallow and return whatever was collected
        }
        return list;
    }
}
