<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="utf-8">
    <title>Login - VulnLab</title>
    <link href="jumbotron.css" rel="stylesheet">
</head>
<body>
<div class="container" style="margin-top:80px; max-width:400px;">
    <h2>Login</h2>
    <!--
      Backing logic: com.webapp.vulnlab.LoginServlet
      VULN-4  SQL Injection auth bypass  - try username: admin' --  (any password)
      VULN-13 Hardcoded backdoor         - try password: backdoor_devsecops_2020
      VULN-14 Plaintext password storage - see DBInitListener seed data
      VULN-15 Log4Shell logging sink     - username is logged via log4j-core 2.14.1
      Full payloads & explanations: VULNERABILITIES.md
    -->
    <form action="login" method="post">
        <div class="form-group">
            <label>Username</label>
            <input type="text" name="username" class="form-control">
        </div>
        <div class="form-group">
            <label>Password</label>
            <input type="password" name="password" class="form-control">
        </div>
        <button type="submit" class="btn btn-success">Sign in</button>
    </form>
    <p><a href="index.jsp">Back to home</a></p>
</div>
</body>
</html>
