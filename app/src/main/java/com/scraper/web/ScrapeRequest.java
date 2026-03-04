package com.scraper.web;

/**
 * Request payload for the scrape API endpoint.
 */
public class ScrapeRequest {
    private String url;
    private String cssSelector;
    private int maxResults;

    public ScrapeRequest() {}

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getCssSelector() { return cssSelector; }
    public void setCssSelector(String cssSelector) { this.cssSelector = cssSelector; }

    public int getMaxResults() { return maxResults; }
    public void setMaxResults(int maxResults) { this.maxResults = maxResults; }
}
