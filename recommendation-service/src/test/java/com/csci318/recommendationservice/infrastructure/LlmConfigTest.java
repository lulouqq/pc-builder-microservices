package com.csci318.recommendationservice.infrastructure;

import dev.langchain4j.model.chat.ChatModel;
import org.junit.jupiter.api.Test;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The chat model is created from configuration only; no network call is made when it is built.
 */
class LlmConfigTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            // Spring Boot's conversion service converts "30s" to a Duration, as in the running application
            .withInitializer(context -> context.getBeanFactory()
                    .setConversionService(ApplicationConversionService.getSharedInstance()))
            .withUserConfiguration(LlmConfig.class)
            .withPropertyValues("recommendation.llm.base-url=https://llm.example.test/v1",
                    "recommendation.llm.timeout=30s");

    @Test
    void createsChatModelWhenApiKeyAndModelAreConfigured() {
        runner.withPropertyValues("recommendation.llm.api-key=test-key", "recommendation.llm.model-name=test-model")
                .run(context -> assertThat(context).hasSingleBean(ChatModel.class));
    }

    @Test
    void noChatModelWithoutApiKey() {
        runner.withPropertyValues("recommendation.llm.api-key=", "recommendation.llm.model-name=test-model")
                .run(context -> assertThat(context).doesNotHaveBean(ChatModel.class));
    }

    @Test
    void noChatModelWithoutModelName() {
        runner.withPropertyValues("recommendation.llm.api-key=test-key", "recommendation.llm.model-name=")
                .run(context -> assertThat(context).doesNotHaveBean(ChatModel.class));
    }
}
