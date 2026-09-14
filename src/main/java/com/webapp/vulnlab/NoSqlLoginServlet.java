package com.webapp.vulnlab;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;

/**
 * VULN-12: NoSQL Injection (CWE-943)
 *
 * *** REQUIRES A REAL MONGODB SERVER (env var MONGO_URI) TO ACTUALLY RUN. ***
 * On this Tomcat instance, with no MongoDB configured, this endpoint will
 * simply fail to connect. It is included so the ANTI-PATTERN is present in
 * the codebase for Semgrep/manual review, and so you can spin up a local
 * MongoDB later to see it exploited live.
 *
 * The bug: the raw JSON request body is parsed straight into a MongoDB
 * query filter with Document.parse(), instead of extracting a plain
 * username/password string and comparing them explicitly. A normal login
 * POST body looks like:
 *   {"username": "alice", "password": "alicepass123"}
 *
 * But because the whole object becomes the filter, an attacker can send
 * MongoDB query operators instead of plain strings:
 *   {"username": {"$ne": null}, "password": {"$ne": null}}
 * "$ne": null means "not equal to null", i.e. "match any value" - so this
 * filter matches the FIRST user in the collection and logs the attacker
 * in without knowing any real credentials. Equivalent to SQLi's
 * ' OR '1'='1, just in MongoDB's query language instead of SQL.
 */
@WebServlet("/nosql-login")
public class NoSqlLoginServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        StringBuilder bodyBuilder = new StringBuilder();
        try (BufferedReader reader = req.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                bodyBuilder.append(line);
            }
        }

        resp.setContentType("text/html");
        String mongoUri = System.getenv("MONGO_URI");
        if (mongoUri == null || mongoUri.isEmpty()) {
            resp.getWriter().println(
                    "<h3>NoSQL lab not active - set MONGO_URI to enable this endpoint.</h3>"
                            + "<p>Anti-pattern is present in NoSqlLoginServlet.java for static "
                            + "review even without a live MongoDB server.</p>");
            return;
        }

        // VULN-12: the entire client-controlled JSON body becomes the query
        // filter, unmodified. No field allowlist, no operator stripping,
        // no type validation.
        Document filter = Document.parse(bodyBuilder.toString());

        try (MongoClient client = MongoClients.create(mongoUri)) {
            MongoDatabase db = client.getDatabase("vulnlab");
            MongoCollection<Document> users = db.getCollection("users");

            Document match = users.find(filter).first();
            if (match != null) {
                resp.getWriter().println("<h3>Login successful (NoSQL). Welcome, "
                        + match.getString("username") + "!</h3>");
            } else {
                resp.getWriter().println("<h3>Invalid credentials.</h3>");
            }
        } catch (Exception e) {
            resp.getWriter().println("<h3>NoSQL error: " + e.getMessage() + "</h3>");
        }
    }
}
