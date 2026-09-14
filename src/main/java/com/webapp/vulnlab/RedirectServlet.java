package com.webapp.vulnlab;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * VULN-16 (BONUS): Open Redirect (CWE-601)
 *
 * Redirects to whatever 'url' parameter is supplied, with no check that
 * it stays on this domain. Commonly abused in phishing: attacker sends a
 * victim a link to the real, trusted domain
 * (https://yourapp.com/redirect?url=https://evil.example/fake-login),
 * the victim trusts the domain in the link, but ends up on the attacker's
 * page after the redirect fires.
 */
@WebServlet("/redirect")
public class RedirectServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String url = req.getParameter("url");
        if (url == null || url.isEmpty()) {
            resp.sendRedirect("index.jsp");
            return;
        }

        // VULN-16: no allowlist of internal paths / trusted hosts before redirecting.
        resp.sendRedirect(url);
    }
}
