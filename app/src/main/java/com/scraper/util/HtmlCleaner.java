package com.scraper.util;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;

/**
 * Utility to clean and sanitize raw HTML content.
 */
public final class HtmlCleaner {

    private HtmlCleaner() {
        // utility class
    }

    /**
     * Strips all HTML tags and returns plain text.
     */
    public static String toPlainText(String html) {
        if (html == null || html.isBlank()) {
            return "";
        }
        Document doc = Jsoup.parse(html);
        return doc.text().strip();
    }

    /**
     * Removes script, style, and other non-content tags, returning cleaned HTML.
     */
    public static String cleanHtml(String html) {
        if (html == null || html.isBlank()) {
            return "";
        }
        Document doc = Jsoup.parse(html);
        doc.select("script, style, noscript, iframe, svg, link[rel=stylesheet]").remove();
        return doc.body() != null ? doc.body().html() : "";
    }

    /**
     * Sanitizes HTML to only allow basic safe tags (p, b, i, a, ul, li, h1-h6, etc.).
     */
    public static String sanitize(String html) {
        if (html == null || html.isBlank()) {
            return "";
        }
        return Jsoup.clean(html, Safelist.relaxed());
    }

    /**
     * Extracts all text content from elements matching the given CSS selector.
     */
    public static java.util.List<String> extractTextByCss(String html, String cssSelector) {
        if (html == null || html.isBlank()) {
            return java.util.List.of();
        }
        Document doc = Jsoup.parse(html);
        return doc.select(cssSelector).stream()
                .map(el -> el.text().strip())
                .filter(t -> !t.isEmpty())
                .toList();
    }
}
