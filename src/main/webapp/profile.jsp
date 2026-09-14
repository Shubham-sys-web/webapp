<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="utf-8">
    <title>Profile - VulnLab</title>
    <link href="jumbotron.css" rel="stylesheet">
</head>
<body>
<div class="container" style="margin-top:80px;">
    <h2>My Profile</h2>

    <!-- Server-rendered profile data comes from /profile?id=N (VULN-8: IDOR) -->
    <p>Use the link below to view a profile by id (try changing the number):</p>
    <p><a href="profile?id=1">View profile id=1</a> |
       <a href="profile?id=2">View profile id=2</a> |
       <a href="profile?id=3">View profile id=3</a></p>

    <hr>

    <!--
      VULN-7: DOM-based XSS (CWE-79).
      This banner is built ENTIRELY client-side in JavaScript: it reads
      location.hash (the part of the URL after '#', which is never even
      sent to the server) and inserts it directly into the page with
      innerHTML, with no sanitization. Because the untrusted "source"
      (location.hash) and the dangerous "sink" (innerHTML) are both in the
      browser, this attack is invisible to server-side logs and to
      SearchServlet-style server code review - it can only be found by
      reading the client-side JS or by a DOM-XSS-aware scanner.

      Example: profile.jsp#name=<img src=x onerror=alert(document.cookie)>
    -->
    <div id="welcomeBanner"></div>
    <script>
        (function () {
            var hash = window.location.hash.substring(1); // strip leading '#'
            var params = {};
            hash.split('&').forEach(function (part) {
                var kv = part.split('=');
                if (kv[0]) params[decodeURIComponent(kv[0])] = decodeURIComponent(kv[1] || '');
            });

            if (params.name) {
                // VULN-7: untrusted, URL-derived value written via innerHTML.
                document.getElementById('welcomeBanner').innerHTML =
                    '<p>Welcome back, ' + params.name + '!</p>';
            }
        })();
    </script>

    <p><a href="index.jsp">Back to home</a></p>
</div>
</body>
</html>
