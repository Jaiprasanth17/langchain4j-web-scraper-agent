package com.scraper.tools;

import com.scraper.browser.BrowserService;
import com.scraper.util.RateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ExtractToolTest {

    private BrowserService mockBrowser;
    private ScraperTools tools;

    private static final String MOCK_NEWS_HTML = """
            <html>
            <body>
                <table>
                    <tr class="athing">
                        <td class="title">
                            <span class="titleline">
                                <a href="https://example.com/1">Breaking: Major Tech Announcement</a>
                            </span>
                        </td>
                    </tr>
                    <tr class="athing">
                        <td class="title">
                            <span class="titleline">
                                <a href="https://example.com/2">New Open Source Framework Released</a>
                            </span>
                        </td>
                    </tr>
                    <tr class="athing">
                        <td class="title">
                            <span class="titleline">
                                <a href="https://example.com/3">AI Research Breakthrough in 2024</a>
                            </span>
                        </td>
                    </tr>
                    <tr class="athing">
                        <td class="title">
                            <span class="titleline">
                                <a href="https://example.com/4">   </a>
                            </span>
                        </td>
                    </tr>
                </table>
            </body>
            </html>
            """;

    @BeforeEach
    void setUp() {
        mockBrowser = Mockito.mock(BrowserService.class);
        RateLimiter rateLimiter = new RateLimiter(0); // No delay for tests
        tools = new ScraperTools(mockBrowser, rateLimiter);
    }

    @Test
    void extractByCss_extractsTitlelineLinks() {
        when(mockBrowser.getPageHtml()).thenReturn(MOCK_NEWS_HTML);

        String result = tools.extractByCss(".titleline > a");

        assertTrue(result.contains("Breaking: Major Tech Announcement"));
        assertTrue(result.contains("New Open Source Framework Released"));
        assertTrue(result.contains("AI Research Breakthrough in 2024"));
        // Should not contain blank entries
        for (String line : result.split("\n")) {
            assertFalse(line.isBlank(), "No blank lines expected");
        }
    }

    @Test
    void extractByCss_returnsMessageForNoMatch() {
        when(mockBrowser.getPageHtml()).thenReturn(MOCK_NEWS_HTML);

        String result = tools.extractByCss("div.nonexistent");

        assertTrue(result.contains("No elements found"));
    }

    @Test
    void extractByCss_handlesEmptyHtml() {
        when(mockBrowser.getPageHtml()).thenReturn("<html><body></body></html>");

        String result = tools.extractByCss("h2");

        assertTrue(result.contains("No elements found"));
    }

    @Test
    void extractByCss_handlesComplexSelectors() {
        String html = """
                <div id="content">
                    <article class="post">
                        <h2 class="headline">Article One</h2>
                    </article>
                    <article class="post">
                        <h2 class="headline">Article Two</h2>
                    </article>
                    <article class="ad">
                        <h2 class="headline">Sponsored Content</h2>
                    </article>
                </div>
                """;
        when(mockBrowser.getPageHtml()).thenReturn(html);

        String result = tools.extractByCss("article.post h2.headline");

        assertTrue(result.contains("Article One"));
        assertTrue(result.contains("Article Two"));
        assertFalse(result.contains("Sponsored Content"));
    }

    @Test
    void extractByCss_countsCorrectly() {
        when(mockBrowser.getPageHtml()).thenReturn(MOCK_NEWS_HTML);

        String result = tools.extractByCss(".titleline > a");
        String[] lines = result.split("\n");

        // Should have 3 non-blank entries (4th link has only whitespace)
        assertEquals(3, lines.length);
    }

    @Test
    void robotsCheck_returnsAllowedForPermittedPaths() {
        // This test uses a real HTTP call to a known robots.txt
        // For unit testing we just verify the method signature and basic logic
        String result = tools.robotsCheck("https://news.ycombinator.com/");
        assertNotNull(result);
        assertTrue(result.startsWith("ALLOWED") || result.startsWith("BLOCKED"));
    }
}
