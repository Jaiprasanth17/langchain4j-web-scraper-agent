package com.scraper.agent;

import com.scraper.browser.BrowserService;
import com.scraper.tools.ScraperTools;
import com.scraper.util.RateLimiter;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Builds and configures a ReAct-style LangChain4j agent that uses the
 * scraper tools to navigate and extract data from web pages.
 */
public class ScraperAgent {

    private static final Logger LOG = LoggerFactory.getLogger(ScraperAgent.class);

    private final BrowserService browserService;
    private final ScraperAssistant assistant;

    public ScraperAgent(String openAiApiKey, String modelName) {
        this.browserService = new BrowserService(true);

        RateLimiter rateLimiter = new RateLimiter(2000); // 2 seconds between requests
        ScraperTools tools = new ScraperTools(browserService, rateLimiter);

        OpenAiChatModel chatModel = OpenAiChatModel.builder()
                .apiKey(openAiApiKey)
                .modelName(modelName != null ? modelName : "gpt-4o-mini")
                .temperature(0.0)
                .maxTokens(4096)
                .logRequests(true)
                .logResponses(true)
                .build();

        this.assistant = AiServices.builder(ScraperAssistant.class)
                .chatLanguageModel(chatModel)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(20))
                .tools(tools)
                .build();

        LOG.info("ScraperAgent initialized with model: {}", modelName != null ? modelName : "gpt-4o-mini");
    }

    /**
     * Sends a scraping task prompt to the agent and returns the response.
     */
    public String execute(String prompt) {
        LOG.info("Executing agent with prompt: {}", prompt);
        return assistant.chat(prompt);
    }

    public void close() {
        browserService.close();
    }
}
