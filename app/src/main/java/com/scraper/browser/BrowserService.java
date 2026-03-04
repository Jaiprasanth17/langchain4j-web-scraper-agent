package com.scraper.browser;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Manages a Playwright headless Chromium browser instance.
 * Provides low-level browser operations used by the tool classes.
 */
public class BrowserService implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(BrowserService.class);

    private final Playwright playwright;
    private final Browser browser;
    private BrowserContext context;
    private Page page;

    public BrowserService() {
        this(true);
    }

    public BrowserService(boolean headless) {
        LOG.info("Launching Playwright Chromium (headless={})", headless);
        this.playwright = Playwright.create();
        this.browser = playwright.chromium().launch(
                new BrowserType.LaunchOptions()
                        .setHeadless(headless)
                        .setArgs(List.of(
                                "--disable-blink-features=AutomationControlled",
                                "--no-sandbox",
                                "--disable-setuid-sandbox",
                                "--disable-dev-shm-usage",
                                "--disable-gpu",
                                "--disable-extensions",
                                "--single-process"
                        ))
        );
        this.context = browser.newContext(
                new Browser.NewContextOptions()
                        .setUserAgent("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 " +
                                "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                        .setViewportSize(1280, 720)
        );
        this.page = context.newPage();
        LOG.info("Browser ready");
    }

    public Page getPage() {
        return page;
    }

    public void navigate(String url) {
        LOG.info("Navigating to {}", url);
        page.navigate(url, new Page.NavigateOptions().setTimeout(30_000));
        page.waitForLoadState(LoadState.NETWORKIDLE,
                new Page.WaitForLoadStateOptions().setTimeout(15_000));
    }

    public void click(String selector) {
        LOG.info("Clicking selector: {}", selector);
        page.click(selector, new Page.ClickOptions().setTimeout(10_000));
    }

    public void fill(String selector, String value) {
        LOG.info("Filling selector: {} with value length={}", selector, value.length());
        page.fill(selector, value);
    }

    public void waitForSelector(String selector, int timeoutMs) {
        LOG.info("Waiting for selector: {} (timeout={}ms)", selector, timeoutMs);
        page.waitForSelector(selector,
                new Page.WaitForSelectorOptions().setTimeout(timeoutMs));
    }

    public void scroll(int deltaY) {
        LOG.info("Scrolling by deltaY={}", deltaY);
        page.mouse().wheel(0, deltaY);
        // Give time for lazy-loaded content
        page.waitForTimeout(1000);
    }

    public String extractByCss(String cssSelector) {
        LOG.info("Extracting by CSS: {}", cssSelector);
        var elements = page.querySelectorAll(cssSelector);
        StringBuilder sb = new StringBuilder();
        for (var el : elements) {
            String text = el.textContent();
            if (text != null && !text.isBlank()) {
                sb.append(text.strip()).append("\n");
            }
        }
        return sb.toString().strip();
    }

    public List<String> extractListByCss(String cssSelector) {
        LOG.info("Extracting list by CSS: {}", cssSelector);
        var elements = page.querySelectorAll(cssSelector);
        return elements.stream()
                .map(ElementHandle::textContent)
                .filter(t -> t != null && !t.isBlank())
                .map(String::strip)
                .toList();
    }

    public String getPageHtml() {
        return page.content();
    }

    public String screenshot(String filename) {
        Path dir = Paths.get(System.getProperty("user.dir"), "artifacts");
        try {
            java.nio.file.Files.createDirectories(dir);
        } catch (java.io.IOException e) {
            LOG.warn("Could not create artifacts directory: {}", e.getMessage());
        }
        Path path = dir.resolve(filename);
        page.screenshot(new Page.ScreenshotOptions()
                .setPath(path)
                .setFullPage(true));
        LOG.info("Screenshot saved to {}", path);
        return path.toAbsolutePath().toString();
    }

    public String currentUrl() {
        return page.url();
    }

    @Override
    public void close() {
        LOG.info("Closing browser");
        if (page != null) page.close();
        if (context != null) context.close();
        if (browser != null) browser.close();
        if (playwright != null) playwright.close();
    }
}
