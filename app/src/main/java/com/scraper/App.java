package com.scraper;

import com.scraper.agent.ScraperAgent;
import com.scraper.web.WebServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Main entry point for the LangChain4j Web Scraper Agent.
 *
 * Supports two modes:
 *   - CLI mode (default): Scrapes Hacker News and saves results to artifacts/
 *   - Web mode (--web):   Starts a web UI on http://localhost:8080
 */
public class App {

    private static final Logger LOG = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) {
        String apiKey = System.getenv("OPENAI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            LOG.error("OPENAI_API_KEY environment variable is required");
            System.err.println("ERROR: Please set the OPENAI_API_KEY environment variable.");
            System.err.println("  export OPENAI_API_KEY=sk-your-key-here");
            System.exit(1);
        }

        String model = System.getenv().getOrDefault("OPENAI_MODEL", "gpt-4o-mini");

        // Check for --web flag
        boolean webMode = false;
        int port = 8080;
        for (int i = 0; i < args.length; i++) {
            if ("--web".equals(args[i])) {
                webMode = true;
            } else if ("--port".equals(args[i]) && i + 1 < args.length) {
                port = Integer.parseInt(args[i + 1]);
                i++;
            }
        }

        // Ensure artifacts directory exists
        try {
            Files.createDirectories(Paths.get("artifacts"));
        } catch (IOException e) {
            LOG.warn("Could not create artifacts directory: {}", e.getMessage());
        }

        if (webMode) {
            runWebMode(apiKey, model, port);
        } else {
            runCliMode(apiKey, model);
        }
    }

    private static void runWebMode(String apiKey, String model, int port) {
        LOG.info("=== LangChain4j Web Scraper Agent - Web UI Mode ===");
        try {
            WebServer server = new WebServer(port, apiKey, model);
            server.start();
            System.out.println();
            System.out.println("  Web UI is running at: http://localhost:" + port);
            System.out.println("  Press Ctrl+C to stop.");
            System.out.println();

            // Shutdown hook for clean exit
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                LOG.info("Shutting down web server...");
                server.stop();
            }));

            // Keep main thread alive
            Thread.currentThread().join();
        } catch (IOException e) {
            LOG.error("Failed to start web server: {}", e.getMessage(), e);
            System.exit(1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void runCliMode(String apiKey, String model) {
        LOG.info("=== LangChain4j Web Scraper Agent Demo ===");
        LOG.info("Target: Hacker News (https://news.ycombinator.com)");
        LOG.info("Model: {}", model);

        ScraperAgent agent = null;
        try {
            agent = new ScraperAgent(apiKey, model);

            String prompt = """
                    Scrape the front page of Hacker News at https://news.ycombinator.com

                    Steps:
                    1. First check robots.txt for https://news.ycombinator.com
                    2. Open the page https://news.ycombinator.com
                    3. Extract all article titles using the CSS selector ".titleline > a"
                    4. Take a screenshot named "hackernews_frontpage.png"
                    5. Return the titles as a JSON array of objects with "rank" (number) and "title" (string) fields.
                       Only include the first 15 titles.
                    """;

            String result = agent.execute(prompt);

            LOG.info("=== Agent Response ===");
            System.out.println();
            System.out.println(result);

            // Save output to artifacts
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            Path outputPath = Paths.get("artifacts", "output_" + timestamp + ".json");
            Files.writeString(outputPath, result);
            LOG.info("Output saved to {}", outputPath);

            // Save run log
            Path logPath = Paths.get("artifacts", "run_log_" + timestamp + ".txt");
            Files.writeString(logPath,
                    "Run timestamp: " + timestamp + "\n" +
                    "Model: " + model + "\n" +
                    "Target: https://news.ycombinator.com\n" +
                    "Prompt: " + prompt + "\n" +
                    "Result:\n" + result + "\n");
            LOG.info("Run log saved to {}", logPath);

        } catch (Exception e) {
            LOG.error("Agent execution failed: {}", e.getMessage(), e);
            System.exit(1);
        } finally {
            if (agent != null) {
                agent.close();
            }
        }
    }
}
