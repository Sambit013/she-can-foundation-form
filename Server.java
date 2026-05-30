import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.Headers;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * She Can Foundation – Contact Form Backend
 * Pure Java, zero external dependencies.
 * Uses only the built-in com.sun.net.httpserver package.
 *
 * Compile : javac Server.java
 * Run     : java Server
 * Opens   : http://localhost:8080
 */
public class Server {

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        // Serve the HTML page
        server.createContext("/", new StaticFileHandler());

        // Handle form submission
        server.createContext("/submit", new FormSubmitHandler());

        server.setExecutor(null); // default executor
        System.out.println("✅  Server running at http://localhost:8080");
        server.start();
    }

    // ── Static file handler ──────────────────────────────────────────────────

    static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/") || path.equals("/index.html")) {
                serveFile(exchange, "index.html", "text/html");
            } else {
                sendResponse(exchange, 404, "text/plain", "404 Not Found");
            }
        }

        private void serveFile(HttpExchange ex, String filename, String mime) throws IOException {
            File file = new File(filename);
            if (!file.exists()) {
                sendResponse(ex, 404, "text/plain", "File not found: " + filename);
                return;
            }
            byte[] bytes = Files.readAllBytes(Paths.get(filename));
            ex.getResponseHeaders().set("Content-Type", mime + "; charset=UTF-8");
            ex.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = ex.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    // ── Form submission handler ──────────────────────────────────────────────

    static class FormSubmitHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);

            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "text/plain", "Method Not Allowed");
                return;
            }

            // Read body
            String body;
            try (InputStream is = exchange.getRequestBody()) {
                body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }

            // Parse form fields (application/x-www-form-urlencoded)
            Map<String, String> fields = parseFormData(body);
            String name    = fields.getOrDefault("name",    "").trim();
            String email   = fields.getOrDefault("email",   "").trim();
            String message = fields.getOrDefault("message", "").trim();

            // Basic validation
            if (name.isEmpty() || email.isEmpty() || message.isEmpty()) {
                sendJson(exchange, 400, "{\"success\":false,\"error\":\"All fields are required.\"}");
                return;
            }
            if (!email.matches("^[\\w.+\\-]+@[\\w\\-]+\\.[a-zA-Z]{2,}$")) {
                sendJson(exchange, 400, "{\"success\":false,\"error\":\"Invalid email address.\"}");
                return;
            }

            // Log to console (in production you'd persist to a DB)
            System.out.printf("%n📩 New Submission%n  Name   : %s%n  Email  : %s%n  Message: %s%n",
                    name, email, message);

            // Optionally append to a CSV log
            appendToLog(name, email, message);

            sendJson(exchange, 200, "{\"success\":true,\"message\":\"Form Submitted Successfully\"}");
        }

        private Map<String, String> parseFormData(String data) throws UnsupportedEncodingException {
            Map<String, String> map = new HashMap<>();
            if (data == null || data.isEmpty()) return map;
            for (String pair : data.split("&")) {
                String[] kv = pair.split("=", 2);
                if (kv.length == 2) {
                    String key   = URLDecoder.decode(kv[0], "UTF-8");
                    String value = URLDecoder.decode(kv[1], "UTF-8");
                    map.put(key, value);
                }
            }
            return map;
        }

        private void appendToLog(String name, String email, String message) {
            try (FileWriter fw = new FileWriter("submissions.csv", true)) {
                // Escape commas/quotes in message
                String safeMsg = "\"" + message.replace("\"", "\"\"") + "\"";
                fw.write(name + "," + email + "," + safeMsg + "\n");
            } catch (IOException e) {
                System.err.println("⚠️  Could not write to submissions.csv: " + e.getMessage());
            }
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    static void addCorsHeaders(HttpExchange ex) {
        Headers h = ex.getResponseHeaders();
        h.set("Access-Control-Allow-Origin",  "*");
        h.set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        h.set("Access-Control-Allow-Headers", "Content-Type");
    }

    static void sendResponse(HttpExchange ex, int code, String mime, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", mime + "; charset=UTF-8");
        ex.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    static void sendJson(HttpExchange ex, int code, String json) throws IOException {
        sendResponse(ex, code, "application/json", json);
    }
}
