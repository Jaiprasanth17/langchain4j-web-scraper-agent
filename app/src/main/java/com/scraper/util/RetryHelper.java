package com.scraper.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

/**
 * Generic retry helper with exponential backoff.
 */
public final class RetryHelper {

    private static final Logger LOG = LoggerFactory.getLogger(RetryHelper.class);

    private RetryHelper() {
    }

    /**
     * Retries the given action up to maxRetries times with exponential backoff.
     *
     * @param action     the action to execute
     * @param maxRetries maximum number of retry attempts
     * @param baseDelayMs base delay in milliseconds (doubles each retry)
     * @param <T>        return type
     * @return the result of the action
     */
    public static <T> T withRetry(Supplier<T> action, int maxRetries, long baseDelayMs) {
        Exception lastException = null;
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                return action.get();
            } catch (Exception e) {
                lastException = e;
                if (attempt < maxRetries) {
                    long delay = baseDelayMs * (1L << attempt);
                    LOG.warn("Attempt {} failed: {}. Retrying in {}ms...",
                            attempt + 1, e.getMessage(), delay);
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Retry interrupted", ie);
                    }
                }
            }
        }
        throw new RuntimeException("All " + (maxRetries + 1) + " attempts failed", lastException);
    }

    /**
     * Retries a void action.
     */
    public static void withRetryVoid(Runnable action, int maxRetries, long baseDelayMs) {
        withRetry(() -> {
            action.run();
            return null;
        }, maxRetries, baseDelayMs);
    }
}
