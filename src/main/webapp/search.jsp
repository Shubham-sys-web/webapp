<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="utf-8">
    <title>Search - VulnLab</title>
    <link href="jumbotron.css" rel="stylesheet">
</head>
<body>
<div class="container" style="margin-top:80px;">
    <h2>Product Search</h2>
    <form action="search" method="get">
        <input type="text" name="q" placeholder="Search products...">
        <button type="submit">Search</button>
    </form>

    <%--
      VULN-5: Reflected XSS (CWE-79).
      The 'q' parameter is written directly into the page with <%= %>
      instead of being HTML-escaped first. Because the value came straight
      from the URL/query string and is echoed back unmodified in the same
      response, this is "reflected" (as opposed to Stored/DOM XSS).

      Example: search?q=<script>alert(document.cookie)</script>

      Fix would be to HTML-encode: fn:escapeXml(param.q) via JSTL, or
      org.apache.commons.text.StringEscapeUtils.escapeHtml4(q) in the servlet.
    --%>
    <p>Showing results for: <%= request.getAttribute("query") %></p>

    <% if (request.getAttribute("dbError") != null) { %>
        <%-- VULN-1 continued: raw DB error text surfaced to the client. --%>
        <p style="color:red;">DB error: <%= request.getAttribute("dbError") %></p>
    <% } %>

    <ul>
        <%= request.getAttribute("results") %>
    </ul>

    <p><a href="index.jsp">Back to home</a></p>
</div>
</body>
</html>
