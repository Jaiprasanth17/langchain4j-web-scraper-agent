package com.scraper;

import com.scraper.agent.ScraperAgent;
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
 * Demonstrates scraping article titles from Hacker News (news.ycombinator.com),
 * which allows scraping per its robots.txt.
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

        // Ensure artifacts directory exists
        try {
            Files.createDirectories(Paths.get("artifacts"));
        } catch (IOException e) {
            LOG.warn("Could not create artifacts directory: {}", e.getMessage());
        }

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
