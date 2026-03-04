package com.scraper.web;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.scraper.agent.ScraperAgent;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Embedded HTTP server that provides a web UI for the scraper agent.
 * Uses JDK built-in HttpServer — no extra dependencies needed.
 */
public class WebServer {

    private static final Logger LOG = LoggerFactory.getLogger(WebServer.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final HttpServer server;
    private final String apiKey;
    private final String modelName;
    private final ExecutorService executor = Executors.newFixedThreadPool(4);

    public WebServer(int port, String apiKey, String modelName) throws IOException {
        this.apiKey = apiKey;
        this.modelName = modelName;

        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.setExecutor(executor);

        // Routes
        server.createContext("/", this::handleIndex);
        server.createContext("/api/scrape", this::handleScrape);
        server.createContext("/api/screenshot/", this::handleScreenshot);
        server.createContext("/static/", this::handleStatic);

        LOG.info("WebServer configured on port {}", port);
    }

    public void start() {
        server.start();
        LOG.info("=== Web UI is running at http://localhost:{} ===", server.getAddress().getPort());
    }

    public void stop() {
        server.stop(2);
        executor.shutdown();
        LOG.info("WebServer stopped");
    }

    // --- Route Handlers ---

    private void handleIndex(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "text/plain", "Method Not Allowed");
            return;
        }
        byte[] html = loadResource("/web/index.html");
        sendResponse(exchange, 200, "text/html", html);
    }

    private void handleStatic(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "text/plain", "Method Not Allowed");
            return;
        }
        String path = exchange.getRequestURI().getPath();
        String resourcePath = "/web" + path.substring("/static".length());

        byte[] content = loadResource(resourcePath);
        if (content == null) {
            sendResponse(exchange, 404, "text/plain", "Not Found");
            return;
        }

        String contentType = "application/octet-stream";
        if (resourcePath.endsWith(".css")) contentType = "text/css";
        else if (resourcePath.endsWith(".js")) contentType = "application/javascript";
        else if (resourcePath.endsWith(".png")) contentType = "image/png";
        else if (resourcePath.endsWith(".svg")) contentType = "image/svg+xml";

        sendResponse(exchange, 200, contentType, content);
    }

    private void handleScrape(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "text/plain", "Method Not Allowed");
            return;
        }

        // Enable CORS
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");

        // Read request body
        String body;
        try (InputStream is = exchange.getRequestBody()) {
            body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }

        ScrapeRequest request = GSON.fromJson(body, ScrapeRequest.class);
        if (request.getUrl() == null || request.getUrl().isBlank()) {
            ScrapeResponse errorResp = new ScrapeResponse();
            errorResp.setStatus("error");
            errorResp.setError("URL is required");
            sendJsonResponse(exchange, 400, errorResp);
            return;
        }

        // Set defaults
        String cssSelector = request.getCssSelector();
        if (cssSelector == null || cssSelector.isBlank()) {
            cssSelector = "h1, h2, h3, a";
        }
        int maxResults = request.getMaxResults() > 0 ? request.getMaxResults() : 20;

        // Build prompt dynamically based on user input
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String screenshotName = "scrape_" + timestamp + ".png";

        String prompt = String.format("""
                Scrape the web page at %s
                
                Steps:
                1. First check robots.txt for %s
                2. Open the page %s
                3. Extract content using the CSS selector "%s"
                4. Take a screenshot named "%s"
                5. Return the extracted data as a JSON array of objects.
                   Each object should have an "index" (number) and "text" (string) field.
                   Only include the first %d results.
                   
                IMPORTANT: Return ONLY the JSON array, no other text before or after it.
                """, request.getUrl(), request.getUrl(), request.getUrl(),
                cssSelector, screenshotName, maxResults);

        // Execute scraping
        long startTime = System.currentTimeMillis();
        ScrapeResponse response = new ScrapeResponse();
        ScraperAgent agent = null;

        try {
            agent = new ScraperAgent(apiKey, modelName);
            String result = agent.execute(prompt);
            long duration = System.currentTimeMillis() - startTime;

            // Save output
            Path artifactsDir = Paths.get(System.getProperty("user.dir"), "artifacts");
            Files.createDirectories(artifactsDir);
            Path outputPath = artifactsDir.resolve("output_" + timestamp + ".json");
            Files.writeString(outputPath, result);

            response.setStatus("success");
            response.setResult(result);
            response.setScreenshotPath("/api/screenshot/" + screenshotName);
            response.setDurationMs(duration);

            LOG.info("Scrape completed in {}ms for URL: {}", duration, request.getUrl());
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            LOG.error("Scrape failed for URL: {}", request.getUrl(), e);
            response.setStatus("error");
            response.setError(e.getMessage());
            response.setDurationMs(duration);
        } finally {
            if (agent != null) {
                agent.close();
            }
        }

        sendJsonResponse(exchange, response.getStatus().equals("success") ? 200 : 500, response);
    }

    private void handleScreenshot(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "text/plain", "Method Not Allowed");
            return;
        }

        String path = exchange.getRequestURI().getPath();
        String filename = path.substring("/api/screenshot/".length());

        // Security: prevent path traversal
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            sendResponse(exchange, 400, "text/plain", "Invalid filename");
            return;
        }

        Path screenshotPath = Paths.get(System.getProperty("user.dir"), "artifacts", filename);
        if (!Files.exists(screenshotPath)) {
            sendResponse(exchange, 404, "text/plain", "Screenshot not found");
            return;
        }

        byte[] imageBytes = Files.readAllBytes(screenshotPath);
        sendResponse(exchange, 200, "image/png", imageBytes);
    }

    // --- Helpers ---

    private byte[] loadResource(String resourcePath) {
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is == null) return null;
            return is.readAllBytes();
        } catch (IOException e) {
            LOG.warn("Failed to load resource: {}", resourcePath, e);
            return null;
        }
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String contentType, String body) throws IOException {
        sendResponse(exchange, statusCode, contentType, body.getBytes(StandardCharsets.UTF_8));
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String contentType, byte[] body) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(statusCode, body.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(body);
        }
    }

    private void sendJsonResponse(HttpExchange exchange, int statusCode, Object obj) throws IOException {
        String json = GSON.toJson(obj);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
