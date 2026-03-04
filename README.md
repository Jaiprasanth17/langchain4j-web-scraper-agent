# LangChain4j Web Scraper Agent

A Java-based web scraping agent powered by [LangChain4j](https://docs.langchain4j.dev/) and [Playwright](https://playwright.dev/java/). The agent uses a ReAct (Reasoning + Acting) pattern to autonomously navigate websites and extract structured data using a headless Chromium browser.

## Architecture

```
┌─────────────────────────────────────────────┐
│                  App (Main)                  │
├─────────────────────────────────────────────┤
│              ScraperAgent                    │
│  ┌─────────────┐  ┌──────────────────────┐  │
│  │ OpenAI LLM  │  │  ScraperAssistant    │  │
│  │ (gpt-4o-mini│  │  (ReAct Agent)       │  │
│  └─────────────┘  └──────────────────────┘  │
├─────────────────────────────────────────────┤
│              ScraperTools                    │
│  robotsCheck │ openUrl │ extractByCss │ ... │
├─────────────────────────────────────────────┤
│  BrowserService (Playwright Headless)       │
├──────────┬──────────┬───────────────────────┤
│HtmlCleaner│RateLimiter│  RetryHelper        │
└──────────┴──────────┴───────────────────────┘
```

## Prerequisites

- **Java 21** or later
- **Gradle** (wrapper included)
- **OpenAI API Key**

## Setup

1. **Clone the repository:**
   ```bash
   git clone <repo-url>
   cd langchain4j-web-scraper-agent
   ```

2. **Set environment variables:**
   ```bash
   export OPENAI_API_KEY=sk-your-openai-api-key
   export OPENAI_MODEL=gpt-4o-mini    # optional, defaults to gpt-4o-mini
   ```

3. **Install Playwright browsers** (done automatically on `./gradlew run`):
   ```bash
   ./gradlew installPlaywright
   ```

## Run Instructions

### Run the demo
```bash
./gradlew run
```

This will:
1. Launch a headless Chromium browser
2. Check robots.txt compliance for the target site
3. Navigate to Hacker News and extract article titles
4. Save JSON output and screenshots to the `artifacts/` directory

### Run tests
```bash
./gradlew test
```

### Build only
```bash
./gradlew build
```

## Environment Variables

| Variable        | Required | Default       | Description                          |
|----------------|----------|---------------|--------------------------------------|
| `OPENAI_API_KEY` | Yes      | —             | OpenAI API key for the LLM provider |
| `OPENAI_MODEL`   | No       | `gpt-4o-mini` | OpenAI model name                    |

## Available Tools

The agent has access to the following tools:

| Tool           | Description                                                    |
|---------------|----------------------------------------------------------------|
| `robotsCheck` | Checks robots.txt to verify a URL path is allowed for scraping |
| `openUrl`     | Opens a URL in the headless browser                            |
| `click`       | Clicks an element by CSS selector                              |
| `fill`        | Fills a form field by CSS selector                             |
| `waitFor`     | Waits for an element to appear on the page                     |
| `scroll`      | Scrolls the page to trigger lazy-loaded content                |
| `extractByCss`| Extracts text from elements matching a CSS selector            |
| `screenshot`  | Takes a full-page screenshot                                   |

## Compliance Guardrails

This agent is designed with responsible scraping practices:

1. **robots.txt Compliance:** The agent ALWAYS checks robots.txt before scraping any URL. If a path is disallowed, the agent will refuse to proceed.

2. **Rate Limiting:** A built-in rate limiter enforces a minimum 2-second delay between browser requests to avoid overwhelming target servers.

3. **Retry with Backoff:** Failed operations use exponential backoff (base 2s, up to 3 attempts) to handle transient errors gracefully.

4. **Polite User-Agent:** The browser identifies itself with a standard user agent string.

5. **Terms of Service:** Users are responsible for ensuring compliance with the target website's Terms of Service. The demo uses Hacker News, which permits scraping of its front page.

## Project Structure

```
app/src/main/java/com/scraper/
├── App.java                    # Main entry point
├── agent/
│   ├── ScraperAgent.java       # Agent orchestrator
│   └── ScraperAssistant.java   # LangChain4j AI Service interface
├── browser/
│   └── BrowserService.java     # Playwright browser wrapper
├── tools/
│   └── ScraperTools.java       # @Tool-annotated methods
└── util/
    ├── HtmlCleaner.java        # HTML sanitization utilities
    ├── RateLimiter.java        # Request rate limiting
    └── RetryHelper.java        # Retry with exponential backoff
```

## Output

The demo produces:
- JSON output printed to stdout
- `artifacts/output_<timestamp>.json` — extracted data
- `artifacts/run_log_<timestamp>.txt` — execution log
- `artifacts/hackernews_frontpage.png` — screenshot of the scraped page

## License

MIT
