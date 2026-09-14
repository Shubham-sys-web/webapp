package com.webapp.vulnlab;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * VULN-10: Path Traversal (CWE-22)
 *
 * 'filename' is appended directly onto a base directory with no
 * normalization/allowlist check and no verification that the resolved
 * path stays inside that base directory. Supplying "../" sequences lets
 * an attacker escape the intended uploads/ folder and read arbitrary
 * files the Tomcat OS user can access (e.g. /etc/passwd, application
 * config files, etc).
 *
 * Example: /download?filename=../../../../etc/passwd
 */
@WebServlet("/download")
public class DownloadServlet extends HttpServlet {

    // Intended "sandbox" directory - but nothing below enforces staying inside it.
    private static final String BASE_DIR = "/opt/tomcat/webapps/WebApp/uploads/";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String filename = req.getParameter("filename");
        if (filename == null) {
            resp.getWriter().println("<h3>Missing filename parameter.</h3>");
            return;
        }

        // VULN-10: no sanitization of "../", no canonical-path check against BASE_DIR.
        File file = new File(BASE_DIR + filename);

        if (!file.exists() || !file.isFile()) {
            resp.getWriter().println("<h3>File not found: " + filename + "</h3>");
            return;
        }

        resp.setContentType("application/octet-stream");
        resp.setHeader("Content-Disposition", "attachment; filename=\"" + file.getName() + "\"");

        try (FileInputStream in = new FileInputStream(file);
             OutputStream out = resp.getOutputStream()) {
            byte[] buffer = new byte[4096];
            int len;
            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
        }
    }
}
