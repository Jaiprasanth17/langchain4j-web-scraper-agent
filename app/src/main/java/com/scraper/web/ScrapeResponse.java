package com.scraper.web;

/**
 * Response payload from the scrape API endpoint.
 */
public class ScrapeResponse {
    private String status;
    private String result;
    private String screenshotPath;
    private String error;
    private long durationMs;

    public ScrapeResponse() {}

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }

    public String getScreenshotPath() { return screenshotPath; }
    public void setScreenshotPath(String screenshotPath) { this.screenshotPath = screenshotPath; }

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }

    public long getDurationMs() { return durationMs; }
    public void setDurationMs(long durationMs) { this.durationMs = durationMs; }
}
