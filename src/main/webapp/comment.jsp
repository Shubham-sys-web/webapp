<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.List" %>
<%@ page import="com.webapp.vulnlab.CommentServlet" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="utf-8">
    <title>Guestbook - VulnLab</title>
    <link href="jumbotron.css" rel="stylesheet">
</head>
<body>
<div class="container" style="margin-top:80px;">
    <h2>Guestbook</h2>

    <form action="comment" method="post">
        <div class="form-group">
            <input type="text" name="author" placeholder="Your name" class="form-control">
        </div>
        <div class="form-group">
            <textarea name="message" placeholder="Your comment" class="form-control"></textarea>
        </div>
        <button type="submit" class="btn btn-primary">Post comment</button>
    </form>

    <hr>
    <h3>Comments</h3>
    <%
        List<String[]> comments = CommentServlet.getAllComments();
        for (String[] c : comments) {
    %>
        <div style="border-bottom:1px solid #ccc; padding:8px 0;">
            <%--
              VULN-6 continued (Stored XSS): c[1] (the message) is written
              directly with <%= %>, no HTML-escaping. Whatever was saved in
              CommentServlet.doPost() - including <script> tags - executes
              here for every visitor of this page, not just the person who
              posted it. That persistence + "affects other users" combo is
              what distinguishes Stored XSS from Reflected XSS (search.jsp).

              Fix: use JSTL <c:out value="${c[1]}"/> or
              StringEscapeUtils.escapeHtml4(c[1]) before output.
            --%>
            <b><%= c[0] %>:</b> <%= c[1] %>
        </div>
    <%
        }
    %>

    <p><a href="index.jsp">Back to home</a></p>
</div>
</body>
</html>
