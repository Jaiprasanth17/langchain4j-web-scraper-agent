# Evaluation: Agent Behavior and Edge Cases

This document describes how the LangChain4j Web Scraper Agent handles key scenarios including pagination, lazy loading, and selector failures.

## 1. Pagination Handling

### Approach
The agent handles pagination through its ReAct reasoning loop. When instructed to scrape multiple pages, the agent:

1. **Identifies pagination controls** — The agent can locate "Next" buttons, page number links, or "Load More" buttons using CSS selectors.
2. **Clicks to navigate** — Uses the `click` tool to advance to the next page.
3. **Extracts per-page data** — Runs `extractByCss` on each page and accumulates results.
4. **Terminates** — Stops when no more pagination controls are found or a page limit is reached.

### Limitations
- The agent relies on the LLM's reasoning to decide when to stop paginating. Without explicit instructions (e.g., "scrape 5 pages"), it may stop after the first page or attempt too many.
- Infinite scroll sites are better handled via the `scroll` tool (see below).

### Recommendation
For reliable multi-page scraping, include explicit instructions in the prompt:
```
"Scrape the first 3 pages. Click the 'More' link at the bottom to paginate."
```

## 2. Lazy Loading Handling

### Approach
Many modern sites load content dynamically as the user scrolls. The agent handles this via:

1. **`scroll` tool** — Scrolls the page by a configurable pixel amount (e.g., 1000px) to trigger `IntersectionObserver`-based lazy loaders.
2. **`waitFor` tool** — After scrolling, waits for new elements to appear in the DOM.
3. **Iterative scrolling** — The agent can be instructed to scroll multiple times until content stops loading.

### Example prompt for lazy-loaded sites:
```
"Scroll down 3 times (1000px each), waiting 2 seconds between scrolls,
then extract all article titles."
```

### Limitations
- The 1-second post-scroll wait is fixed. Some sites with slow APIs may need longer waits.
- Content loaded via user-triggered actions (e.g., button clicks) requires explicit `click` instructions.
- Shadow DOM content is not directly accessible via standard CSS selectors.

## 3. Selector Failures

### Approach
When a CSS selector returns no results, the agent:

1. **Reports the failure** — The `extractByCss` tool returns `"No elements found for selector: ..."`.
2. **LLM reasons about alternatives** — The ReAct loop allows the agent to try different selectors based on the page structure.
3. **Retries** — The `RetryHelper` provides automatic retry with exponential backoff for transient DOM timing issues.

### Common failure scenarios and handling:

| Scenario | Agent Behavior |
|----------|---------------|
| Selector typo | Agent receives "No elements found" and may try alternatives |
| Content not yet loaded | `waitFor` tool can be used before extraction |
| Dynamic class names (e.g., CSS modules) | Agent should use attribute or structural selectors instead |
| Page structure changed | Agent reports failure; prompt should be updated |

### Mitigation strategies:
- Use broad selectors initially (e.g., `h2` instead of `h2.specific-class`).
- Chain `waitFor` before `extractByCss` for dynamic content.
- Include fallback selectors in the prompt.

## 4. Rate Limiting and Error Recovery

### Rate Limiting
- A `RateLimiter` enforces a minimum 2-second gap between requests.
- This is applied to `openUrl`, `click`, and other navigation actions.
- The delay is configurable per instance.

### Error Recovery
- `RetryHelper` wraps tool operations with up to 3 attempts.
- Exponential backoff: 2s → 4s → 8s between retries.
- Non-retryable errors (e.g., invalid selector syntax) propagate immediately.

## 5. robots.txt Compliance

- The agent **always** checks robots.txt before scraping.
- The check parses `Disallow` rules for the wildcard (`*`) user-agent.
- If a path is blocked, the agent refuses to proceed and informs the user.
- If robots.txt is unreachable (404, network error), the agent proceeds cautiously with a warning.

## 6. Known Limitations

1. **No JavaScript rendering analysis** — The agent cannot inspect JS source code to understand dynamic behavior.
2. **No CAPTCHA solving** — Sites with CAPTCHAs cannot be scraped.
3. **Single-tab browsing** — The agent operates in a single browser tab; parallel scraping is not supported.
4. **Token limits** — Very large pages may exceed the LLM's context window when the full HTML is processed.
5. **Headed mode** — Not available in server/CI environments; the agent runs headless only.
