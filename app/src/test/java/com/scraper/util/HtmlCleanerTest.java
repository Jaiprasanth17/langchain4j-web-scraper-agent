package com.scraper.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HtmlCleanerTest {

    private static final String MOCK_HTML = """
            <html>
            <head>
                <script>var x = 1;</script>
                <style>.foo { color: red; }</style>
            </head>
            <body>
                <h2 class="title">First Article Title</h2>
                <p>Some paragraph text.</p>
                <h2 class="title">Second Article Title</h2>
                <div class="content">
                    <h2 class="title">Third Article Title</h2>
                    <script>alert('xss');</script>
                </div>
                <h2 class="title">   </h2>
            </body>
            </html>
            """;

    @Test
    void toPlainText_removesAllTags() {
        String result = HtmlCleaner.toPlainText(MOCK_HTML);
        assertFalse(result.contains("<"));
        assertFalse(result.contains(">"));
        assertTrue(result.contains("First Article Title"));
        assertTrue(result.contains("Some paragraph text."));
    }

    @Test
    void toPlainText_handlesNullAndBlank() {
        assertEquals("", HtmlCleaner.toPlainText(null));
        assertEquals("", HtmlCleaner.toPlainText(""));
        assertEquals("", HtmlCleaner.toPlainText("   "));
    }

    @Test
    void cleanHtml_removesScriptAndStyle() {
        String result = HtmlCleaner.cleanHtml(MOCK_HTML);
        assertFalse(result.contains("<script>"));
        assertFalse(result.contains("<style>"));
        assertFalse(result.contains("var x = 1"));
        assertFalse(result.contains("alert('xss')"));
        assertTrue(result.contains("First Article Title"));
    }

    @Test
    void cleanHtml_handlesNullAndBlank() {
        assertEquals("", HtmlCleaner.cleanHtml(null));
        assertEquals("", HtmlCleaner.cleanHtml(""));
    }

    @Test
    void sanitize_allowsBasicTags() {
        String html = "<p>Hello <b>world</b></p><script>evil()</script>";
        String result = HtmlCleaner.sanitize(html);
        assertTrue(result.contains("<p>"));
        assertTrue(result.contains("<b>world</b>"));
        assertFalse(result.contains("<script>"));
    }

    @Test
    void sanitize_handlesNullAndBlank() {
        assertEquals("", HtmlCleaner.sanitize(null));
        assertEquals("", HtmlCleaner.sanitize(""));
    }

    @Test
    void extractTextByCss_extractsMatchingElements() {
        List<String> results = HtmlCleaner.extractTextByCss(MOCK_HTML, "h2.title");
        assertEquals(3, results.size());
        assertEquals("First Article Title", results.get(0));
        assertEquals("Second Article Title", results.get(1));
        assertEquals("Third Article Title", results.get(2));
    }

    @Test
    void extractTextByCss_returnsEmptyForNoMatch() {
        List<String> results = HtmlCleaner.extractTextByCss(MOCK_HTML, "h3.nonexistent");
        assertTrue(results.isEmpty());
    }

    @Test
    void extractTextByCss_handlesNullAndBlank() {
        assertTrue(HtmlCleaner.extractTextByCss(null, "h2").isEmpty());
        assertTrue(HtmlCleaner.extractTextByCss("", "h2").isEmpty());
    }

    @Test
    void extractTextByCss_filtersBlankEntries() {
        // The mock HTML has an h2.title with only whitespace content
        List<String> results = HtmlCleaner.extractTextByCss(MOCK_HTML, "h2.title");
        for (String r : results) {
            assertFalse(r.isBlank(), "Should not contain blank entries");
        }
    }

    @Test
    void extractTextByCss_handlesNestedContent() {
        String html = """
                <div>
                    <article><h2>Title <span>with span</span></h2></article>
                    <article><h2>Simple Title</h2></article>
                </div>
                """;
        List<String> results = HtmlCleaner.extractTextByCss(html, "article h2");
        assertEquals(2, results.size());
        assertEquals("Title with span", results.get(0));
        assertEquals("Simple Title", results.get(1));
    }
}
