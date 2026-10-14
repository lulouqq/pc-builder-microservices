package com.csci318.recommendationservice.infrastructure;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Creates the LangChain4j chat model for an OpenAI-compatible chat API. The base URL, API key, model name and
 * timeout come from configuration (LLM_BASE_URL, LLM_API_KEY, LLM_MODEL_NAME, LLM_TIMEOUT); nothing is hardcoded.
 * Without an API key and model name no model is created and R1 answers 503 ServiceUnavailable.
 */
@Configuration
public class LlmConfig {

    @Bean
    @ConditionalOnExpression("!'${recommendation.llm.api-key:}'.isBlank() and !'${recommendation.llm.model-name:}'.isBlank()")
    public ChatModel chatModel(@Value("${recommendation.llm.base-url}") String baseUrl,
                               @Value("${recommendation.llm.api-key}") String apiKey,
                               @Value("${recommendation.llm.model-name}") String modelName,
                               @Value("${recommendation.llm.timeout}") Duration timeout) {
        return OpenAiChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(modelName)
                .timeout(timeout)
                // A technical failure or timeout is reported as 503; invalid answers are retried by the service.
                .maxRetries(0)
                .build();
    }
}
