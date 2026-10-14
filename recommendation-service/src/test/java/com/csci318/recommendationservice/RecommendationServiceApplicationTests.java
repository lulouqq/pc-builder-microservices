package com.csci318.recommendationservice;

import dev.langchain4j.model.chat.ChatModel;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class RecommendationServiceApplicationTests {

    @Autowired
    private ApplicationContext context;

    /** The service starts without an LLM API key; no chat model is created then. */
    @Test
    void contextLoadsWithoutLlmConfiguration() {
        assertThat(context.getBeanNamesForType(ChatModel.class)).isEmpty();
    }
}
