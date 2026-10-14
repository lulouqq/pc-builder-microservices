package com.csci318.recommendationservice.infrastructure;

import com.csci318.recommendationservice.TestCatalog;
import com.csci318.recommendationservice.service.BuildContext;
import com.csci318.recommendationservice.service.BuildProposal;
import com.csci318.recommendationservice.service.ServiceUnavailableException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LangChain4jBuildAdvisorTest {

    private static final BuildContext CONTEXT = new BuildContext(1500f, "gaming", "Prefer AMD", null,
            List.of(TestCatalog.CPU_AM5, TestCatalog.MOTHERBOARD));

    private final ChatModel chatModel = mock(ChatModel.class);
    private final LangChain4jBuildAdvisor advisor = new LangChain4jBuildAdvisor(Optional.of(chatModel), new ObjectMapper());

    @Test
    void sendsContextAndParsesJsonAnswer() {
        when(chatModel.chat(anyList())).thenReturn(answer("""
                {"productIds": ["cpu-am5", "mb-b650"], "description": "AM5 build", "reasoning": "Fits", "extra": 1}"""));

        BuildProposal proposal = advisor.proposeBuild(CONTEXT);

        assertThat(proposal.readable()).isTrue();
        assertThat(proposal.productIds()).containsExactly("cpu-am5", "mb-b650");
        assertThat(proposal.description()).isEqualTo("AM5 build");
        assertThat(proposal.reasoning()).isEqualTo("Fits");

        List<ChatMessage> messages = sentMessages();
        assertThat(messages).hasSize(2);
        assertThat(((SystemMessage) messages.get(0)).text()).contains("exactly one product for each component type");
        String userText = ((UserMessage) messages.get(1)).singleText();
        assertThat(userText)
                .contains("Budget: 1500.00")
                .contains("Intended use: gaming")
                .contains("Preference in this request: Prefer AMD")
                .contains("Saved preference: none")
                .contains("id=cpu-am5; name=Ryzen 5 7600; brand=AMD; price=200.00; specifications: Socket=AM5");
    }

    @Test
    void readsJsonInsideMarkdownFence() {
        when(chatModel.chat(anyList())).thenReturn(answer("""
                Here is the build:
                ```json
                {"productIds": ["cpu-am5"], "description": "d", "reasoning": "r"}
                ```"""));

        assertThat(advisor.proposeBuild(CONTEXT).productIds()).containsExactly("cpu-am5");
    }

    @Test
    void answerWithoutJsonIsUnreadable() {
        when(chatModel.chat(anyList())).thenReturn(answer("Sorry, I cannot help with that."));

        BuildProposal proposal = advisor.proposeBuild(CONTEXT);

        assertThat(proposal.readable()).isFalse();
        assertThat(proposal.rawAnswer()).isEqualTo("Sorry, I cannot help with that.");
    }

    @Test
    void reviseSendsPreviousAnswerAndProblems() {
        when(chatModel.chat(anyList())).thenReturn(answer("{\"productIds\": [], \"description\": \"d\", \"reasoning\": \"r\"}"));

        advisor.reviseBuild(CONTEXT, BuildProposal.unreadable("not json"), List.of("Problem one.", "Problem two."));

        List<ChatMessage> messages = sentMessages();
        assertThat(messages).hasSize(4);
        assertThat(((AiMessage) messages.get(2)).text()).isEqualTo("not json");
        assertThat(((UserMessage) messages.get(3)).singleText())
                .contains("- Problem one.")
                .contains("- Problem two.");
    }

    @Test
    void technicalFailureIsServiceUnavailable() {
        when(chatModel.chat(anyList())).thenThrow(new RuntimeException("Read timed out"));

        assertThatThrownBy(() -> advisor.proposeBuild(CONTEXT)).isInstanceOf(ServiceUnavailableException.class);
    }

    @Test
    void missingModelIsServiceUnavailable() {
        LangChain4jBuildAdvisor unconfigured = new LangChain4jBuildAdvisor(Optional.empty(), new ObjectMapper());

        assertThatThrownBy(() -> unconfigured.proposeBuild(CONTEXT))
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessageContaining("LLM_API_KEY");
    }

    @SuppressWarnings("unchecked")
    private List<ChatMessage> sentMessages() {
        ArgumentCaptor<List<ChatMessage>> captor = ArgumentCaptor.forClass(List.class);
        verify(chatModel).chat(captor.capture());
        return captor.getValue();
    }

    private static ChatResponse answer(String text) {
        return ChatResponse.builder().aiMessage(AiMessage.from(text)).build();
    }
}
