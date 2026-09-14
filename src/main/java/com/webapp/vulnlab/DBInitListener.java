package com.webapp.vulnlab;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

/**
 * ============================================================================
 *  DEVSECOPS VULN-LAB - INTENTIONALLY VULNERABLE TRAINING CODE
 * ============================================================================
 *  This entire com.webapp.vulnlab package exists ONLY to practice AppSec
 *  tooling (Semgrep, Snyk, OWASP Dependency-Check) and manual testing
 *  (Burp/ZAP) skills. It seeds an in-memory H2 database used by the
 *  vulnerable servlets. This is NOT production code.
 * ============================================================================
 *
 * Creates:
 *   users(id, username, password, role, ssn)   - plaintext passwords (VULN-14)
 *   products(id, name, description, price, category)
 *   comments(id, author, message)              - used by Stored XSS demo
 *
 * DB stays alive for the whole application lifetime via DB_CLOSE_DELAY=-1,
 * so no external database server is required.
 */
public class DBInitListener implements ServletContextListener {

    public static final String JDBC_URL = "jdbc:h2:mem:vulnlabdb;DB_CLOSE_DELAY=-1";
    public static final String DB_USER = "sa";
    public static final String DB_PASS = "";

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        try {
            Class.forName("org.h2.Driver");
            try (Connection conn = DriverManager.getConnection(JDBC_URL, DB_USER, DB_PASS);
                 Statement st = conn.createStatement()) {

                st.execute("CREATE TABLE IF NOT EXISTS users (" +
                        "id INT PRIMARY KEY AUTO_INCREMENT, " +
                        "username VARCHAR(50) UNIQUE, " +
                        "password VARCHAR(100), " +   // VULN-14: plaintext, no hashing
                        "role VARCHAR(20), " +
                        "ssn VARCHAR(20))");

                st.execute("CREATE TABLE IF NOT EXISTS products (" +
                        "id INT PRIMARY KEY AUTO_INCREMENT, " +
                        "name VARCHAR(100), " +
                        "description VARCHAR(255), " +
                        "price DECIMAL(10,2), " +
                        "category VARCHAR(50))");

                st.execute("CREATE TABLE IF NOT EXISTS comments (" +
                        "id INT PRIMARY KEY AUTO_INCREMENT, " +
                        "author VARCHAR(50), " +
                        "message VARCHAR(1000))");

                // Seed users - plaintext passwords on purpose (VULN-14)
                st.execute("MERGE INTO users (id, username, password, role, ssn) VALUES " +
                        "(1, 'admin', 'admin123', 'admin', '123-45-6789')");
                st.execute("MERGE INTO users (id, username, password, role, ssn) VALUES " +
                        "(2, 'alice', 'alicepass123', 'user', '987-65-4321')");
                st.execute("MERGE INTO users (id, username, password, role, ssn) VALUES " +
                        "(3, 'bob', 'bobpass456', 'user', '555-66-7777')");

                // Seed products
                st.execute("MERGE INTO products (id, name, description, price, category) VALUES " +
                        "(1, 'Laptop', 'DevSecOps edition laptop', 799.99, 'electronics')");
                st.execute("MERGE INTO products (id, name, description, price, category) VALUES " +
                        "(2, 'Keyboard', 'Mechanical keyboard', 49.99, 'electronics')");
                st.execute("MERGE INTO products (id, name, description, price, category) VALUES " +
                        "(3, 'Notebook', 'Plain paper notebook', 2.99, 'stationery')");

                System.out.println("[VULN-LAB] H2 in-memory DB initialized with seed data.");
            }
        } catch (Exception e) {
            System.err.println("[VULN-LAB] Failed to initialize DB: " + e.getMessage());
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        // no-op - in-memory DB is discarded when the app context stops
    }
}
