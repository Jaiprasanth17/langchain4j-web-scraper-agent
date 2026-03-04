package com.scraper.tools;

import com.scraper.browser.BrowserService;
import com.scraper.util.HtmlCleaner;
import com.scraper.util.RateLimiter;
import com.scraper.util.RetryHelper;
import dev.langchain4j.agent.tool.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.util.List;

/**
 * LangChain4j tool methods that the ReAct agent can invoke.
 * Each method is annotated with @Tool so the framework can discover it.
 */
public class ScraperTools {

    private static final Logger LOG = LoggerFactory.getLogger(ScraperTools.class);

    private final BrowserService browserService;
    private final RateLimiter rateLimiter;

    public ScraperTools(BrowserService browserService, RateLimiter rateLimiter) {
        this.browserService = browserService;
        this.rateLimiter = rateLimiter;
    }

    @Tool("Checks robots.txt for the given URL to verify the path is allowed for scraping. Returns 'ALLOWED' or 'BLOCKED' with details.")
    public String robotsCheck(String url) {
        LOG.info("Checking robots.txt for: {}", url);
        try {
            URI uri = URI.create(url);
            String robotsUrl = uri.getScheme() + "://" + uri.getHost() + "/robots.txt";
            String path = uri.getPath();
            if (path == null || path.isEmpty()) {
                path = "/";
            }

            URL robotsURL = URI.create(robotsUrl).toURL();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(robotsURL.openStream()))) {
                String line;
                boolean inWildcardAgent = false;
                while ((line = reader.readLine()) != null) {
                    line = line.strip().toLowerCase();
                    if (line.startsWith("user-agent:")) {
                        String agent = line.substring("user-agent:".length()).strip();
                        inWildcardAgent = agent.equals("*");
                    } else if (inWildcardAgent && line.startsWith("disallow:")) {
                        String disallowed = line.substring("disallow:".length()).strip();
                        if (!disallowed.isEmpty() && path.startsWith(disallowed)) {
                            return "BLOCKED: Path '" + path + "' is disallowed by robots.txt rule: " + disallowed;
                        }
                    }
                }
            }
            return "ALLOWED: Path '" + path + "' is permitted by robots.txt";
        } catch (Exception e) {
            LOG.warn("Could not fetch robots.txt: {}", e.getMessage());
            return "ALLOWED: robots.txt not found or inaccessible (proceeding cautiously)";
        }
    }

    @Tool("Opens a URL in the headless browser and waits for the page to load. Returns the page title.")
    public String openUrl(String url) {
        rateLimiter.acquire();
        return RetryHelper.withRetry(() -> {
            browserService.navigate(url);
            String title = browserService.getPage().title();
            return "Navigated to: " + url + " | Title: " + title;
        }, 2, 2000);
    }

    @Tool("Clicks an element matching the given CSS selector on the current page.")
    public String click(String cssSelector) {
        rateLimiter.acquire();
        return RetryHelper.withRetry(() -> {
            browserService.click(cssSelector);
            return "Clicked: " + cssSelector;
        }, 2, 1000);
    }

    @Tool("Fills a form field matching the given CSS selector with the provided value.")
    public String fill(String cssSelector, String value) {
        return RetryHelper.withRetry(() -> {
            browserService.fill(cssSelector, value);
            return "Filled: " + cssSelector + " with value";
        }, 1, 1000);
    }

    @Tool("Waits for an element matching the CSS selector to appear. timeoutMs is the max wait time in milliseconds.")
    public String waitFor(String cssSelector, int timeoutMs) {
        try {
            browserService.waitForSelector(cssSelector, timeoutMs);
            return "Element found: " + cssSelector;
        } catch (Exception e) {
            return "Timeout waiting for: " + cssSelector + " (" + e.getMessage() + ")";
        }
    }

    @Tool("Scrolls the page down by the given pixel amount to trigger lazy loading of content.")
    public String scroll(int pixels) {
        browserService.scroll(pixels);
        return "Scrolled down by " + pixels + " pixels";
    }

    @Tool("Extracts text content from all elements matching the CSS selector. Returns a newline-separated list of text values.")
    public String extractByCss(String cssSelector) {
        return RetryHelper.withRetry(() -> {
            String html = browserService.getPageHtml();
            List<String> texts = HtmlCleaner.extractTextByCss(html, cssSelector);
            if (texts.isEmpty()) {
                return "No elements found for selector: " + cssSelector;
            }
            return String.join("\n", texts);
        }, 1, 1000);
    }

    @Tool("Takes a full-page screenshot and saves it with the given filename. Returns the file path.")
    public String screenshot(String filename) {
        String path = browserService.screenshot(filename);
        return "Screenshot saved: " + path;
    }
}
