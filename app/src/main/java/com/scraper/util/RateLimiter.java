package com.scraper.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple token-bucket-style rate limiter that enforces a minimum delay between requests.
 */
public class RateLimiter {

    private static final Logger LOG = LoggerFactory.getLogger(RateLimiter.class);

    private final long minDelayMs;
    private long lastRequestTime;

    /**
     * @param minDelayMs minimum milliseconds between consecutive requests
     */
    public RateLimiter(long minDelayMs) {
        this.minDelayMs = minDelayMs;
        this.lastRequestTime = 0;
    }

    /**
     * Blocks the current thread if needed to ensure the minimum delay is respected.
     */
    public synchronized void acquire() {
        long now = System.currentTimeMillis();
        long elapsed = now - lastRequestTime;
        if (elapsed < minDelayMs) {
            long sleepMs = minDelayMs - elapsed;
            LOG.debug("Rate limiter sleeping for {}ms", sleepMs);
            try {
                Thread.sleep(sleepMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        lastRequestTime = System.currentTimeMillis();
    }
}
