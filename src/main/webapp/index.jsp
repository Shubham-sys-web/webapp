<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="description" content="">
    <meta name="author" content="">
    <link rel="icon" href="../../favicon.ico">

    <title>Devsecops Course</title>

    <!-- Custom styles for this template -->
    <link href="jumbotron.css" rel="stylesheet">

    <!-- HTML5 shim and Respond.js IE8 support of HTML5 elements and media queries -->
    <!--[if lt IE 9]>
    <script src="https://oss.maxcdn.com/html5shiv/3.7.2/html5shiv.min.js"></script>
    <script src="https://oss.maxcdn.com/respond/1.4.2/respond.min.js"></script>
    <![endif]-->
</head>

<body>

<div class="navbar navbar-inverse navbar-fixed-top" role="navigation">
    <div class="container">
        <div class="navbar-header">
            <button type="button" class="navbar-toggle" data-toggle="collapse" data-target=".navbar-collapse">
                <span class="sr-only">Toggle navigation</span>
                <span class="icon-bar"></span>
                <span class="icon-bar"></span>
                <span class="icon-bar"></span>
            </button>
            <a class="navbar-brand" href="#">Web App for Developers</a>
        </div>
        <div class="navbar-collapse collapse">
            <form class="navbar-form navbar-right" role="form">
                <div class="form-group">
                    <input type="text" placeholder="Email" class="form-control">
                </div>
                <div class="form-group">
                    <input type="password" placeholder="Password" class="form-control">
                </div>
                <button type="submit" class="btn btn-success">Sign in</button>
            </form>
        </div><!--/.navbar-collapse -->
    </div>
</div>

<!-- Main jumbotron for a primary marketing message or call to action -->
<div class="jumbotron">
    <div class="container">
        <h1>Hello</h1>
        <p>This is from dev branch. </p>
        <p><a class="btn btn-primary btn-lg" role="button">Learn more &raquo;</a></p>
    </div>
</div>

<div class="container">
    <!-- Example row of columns -->
    <div class="row">
        <div class="col-md-4">
            <h2>Heading</h2>
            <p>Donec id elit non mi porta gravida at eget metus. Fusce dapibus, tellus ac cursus commodo, tortor mauris condimentum nibh, ut fermentum massa justo sit amet risus. Etiam porta sem malesuada magna mollis euismod. Donec sed odio dui. </p>
            <p><a class="btn btn-default" href="#" role="button">View details &raquo;</a></p>
        </div>
        <div class="col-md-4">
            <h2>Heading</h2>
            <p>Donec id elit non mi porta gravida at eget metus. Fusce dapibus, tellus ac cursus commodo, tortor mauris condimentum nibh, ut fermentum massa justo sit amet risus. Etiam porta sem malesuada magna mollis euismod. Donec sed odio dui. </p>
            <p><a class="btn btn-default" href="#" role="button">View details &raquo;</a></p>
        </div>
        <div class="col-md-4">
            <h2>Heading</h2>
            <p>Donec sed odio dui. Cras justo odio, dapibus ac facilisis in, egestas eget quam. Vestibulum id ligula porta felis euismod semper. Fusce dapibus, tellus ac cursus commodo, tortor mauris condimentum nibh, ut fermentum massa justo sit amet risus.</p>
            <p><a class="btn btn-default" href="#" role="button">View details &raquo;</a></p>
        </div>
    </div>

    <hr>

    <!--
      ============================================================
      DEVSECOPS VULN-LAB - intentional training vulnerabilities.
      Full write-up, payloads and fixes: VULNERABILITIES.md
      ============================================================
    -->
    <h2>VulnLab - Practice Pages</h2>
    <p>Everything below is intentionally vulnerable for AppSec / Semgrep / Snyk practice.
       See <code>VULNERABILITIES.md</code> in the repo for exploit payloads and the fix for each one.</p>
    <table class="table table-bordered">
        <tr><th>Page / Endpoint</th><th>Vulnerability</th></tr>
        <tr><td><a href="search?q=laptop">/search?q=</a></td><td>SQLi (error-based) + Reflected XSS</td></tr>
        <tr><td><a href="search-blind?id=1">/search-blind?id=</a></td><td>SQLi (blind boolean-based)</td></tr>
        <tr><td><a href="products?category=electronics">/products?category=</a></td><td>SQLi (union-based)</td></tr>
        <tr><td><a href="login.jsp">/login</a></td><td>SQLi auth bypass, hardcoded backdoor, plaintext passwords, Log4Shell logging sink</td></tr>
        <tr><td><a href="comment.jsp">/comment.jsp</a></td><td>Stored XSS (guestbook)</td></tr>
        <tr><td><a href="profile.jsp">/profile.jsp</a></td><td>DOM-based XSS + IDOR (via /profile?id=)</td></tr>
        <tr><td><a href="admin">/admin</a></td><td>Broken Access Control (no auth check)</td></tr>
        <tr><td><a href="download?filename=test.txt">/download?filename=</a></td><td>Path Traversal</td></tr>
        <tr><td><a href="url-preview?url=http://example.com">/url-preview?url=</a></td><td>SSRF</td></tr>
        <tr><td>/nosql-login (POST)</td><td>NoSQL Injection (needs MongoDB)</td></tr>
        <tr><td><a href="redirect?url=https://example.com">/redirect?url=</a></td><td>Open Redirect (bonus)</td></tr>
    </table>

    <footer>
        <p>&copy; Company 2014</p>
    </footer>
</div> <!-- /container -->


<!-- Bootstrap core JavaScript
================================================== -->
<!-- Placed at the end of the document so the pages load faster -->
<script src="https://ajax.googleapis.com/ajax/libs/jquery/1.11.1/jquery.min.js"></script>
<script src="//maxcdn.bootstrapcdn.com/bootstrap/3.2.0/js/bootstrap.min.js"></script>
<link rel="stylesheet" href="//maxcdn.bootstrapcdn.com/bootstrap/3.2.0/css/bootstrap.min.css">
<link rel="stylesheet" href="//maxcdn.bootstrapcdn.com/bootstrap/3.2.0/css/bootstrap-theme.min.css">
</body>
</html>
