package com.csci318.recommendationservice.infrastructure;

import com.csci318.recommendationservice.domain.ComponentType;
import com.csci318.recommendationservice.service.BuildAdvisor;
import com.csci318.recommendationservice.service.BuildContext;
import com.csci318.recommendationservice.service.BuildProposal;
import com.csci318.recommendationservice.service.CatalogProduct;
import com.csci318.recommendationservice.service.ServiceUnavailableException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * The LLM adapter for R1, using LangChain4j's {@link ChatModel}. Java prepares the whole context (budget, intended
 * use, both preferences and the available products) and the LLM only returns the chosen product IDs, a
 * description and the reasoning as JSON. There is no tool calling. The answer is parsed here and checked by
 * the service.
 */
@Component
public class LangChain4jBuildAdvisor implements BuildAdvisor {

    private static final Logger log = LoggerFactory.getLogger(LangChain4jBuildAdvisor.class);

    static final String SYSTEM_PROMPT = """
            You are a PC building assistant for an online PC parts shop.
            Choose exactly one product for each component type: CPU, Motherboard, RAM, GPU, Storage, PSU, Case.
            Rules:
            - Use only product IDs from the list of available products.
            - The total price of the chosen products must not be more than the budget.
            - The CPU and the Motherboard must have the same Socket.
            - The RAM and the Motherboard must have the same Memory Type.
            - The Motherboard and the Case must have the same Form Factor.
            - Choose parts that suit the intended use and follow the customer's preferences.
              The preference in this request is more specific than the saved preference:
              if they conflict, follow the preference in this request.
            Answer with only this JSON object and no other text:
            {"productIds": ["<product id>", ...], "description": "<short summary of the build>", \
            "reasoning": "<why this build suits the customer's budget, intended use and preferences>"}
            """;

    private final Optional<ChatModel> chatModel;
    private final ObjectMapper objectMapper;

    public LangChain4jBuildAdvisor(Optional<ChatModel> chatModel, ObjectMapper objectMapper) {
        this.chatModel = chatModel;
        this.objectMapper = objectMapper;
    }

    @Override
    public BuildProposal proposeBuild(BuildContext context) {
        return ask(List.of(SystemMessage.from(SYSTEM_PROMPT), UserMessage.from(describe(context))));
    }

    @Override
    public BuildProposal reviseBuild(BuildContext context, BuildProposal rejected, List<String> problems) {
        String previousAnswer = rejected.rawAnswer() == null || rejected.rawAnswer().isBlank()
                ? "(empty answer)" : rejected.rawAnswer();
        String feedback = "Your previous answer is not valid:\n"
                + problems.stream().map(problem -> "- " + problem).collect(Collectors.joining("\n"))
                + "\nFix these problems and answer again with only the JSON object.";
        return ask(List.of(SystemMessage.from(SYSTEM_PROMPT), UserMessage.from(describe(context)),
                AiMessage.from(previousAnswer), UserMessage.from(feedback)));
    }

    private BuildProposal ask(List<ChatMessage> messages) {
        ChatModel model = chatModel.orElseThrow(() -> new ServiceUnavailableException(
                "The LLM is not configured (set LLM_API_KEY and LLM_MODEL_NAME)"));
        ChatResponse response;
        try {
            response = model.chat(messages);
        } catch (RuntimeException e) {
            // technical failure: connection error, timeout, authentication or provider error
            log.warn("LLM call failed: {}", e.getMessage());
            throw new ServiceUnavailableException("The LLM is unavailable", e);
        }
        String text = response == null || response.aiMessage() == null ? null : response.aiMessage().text();
        return parse(text);
    }

    /** Reads the JSON object from the answer; tolerates text or Markdown fences around it. */
    BuildProposal parse(String answer) {
        if (answer == null) {
            return BuildProposal.unreadable(null);
        }
        int start = answer.indexOf('{');
        int end = answer.lastIndexOf('}');
        if (start < 0 || end < start) {
            return BuildProposal.unreadable(answer);
        }
        try {
            Answer parsed = objectMapper.readValue(answer.substring(start, end + 1), Answer.class);
            return BuildProposal.of(answer, parsed.productIds(), parsed.description(), parsed.reasoning());
        } catch (JsonProcessingException e) {
            return BuildProposal.unreadable(answer);
        }
    }

    static String describe(BuildContext context) {
        StringBuilder text = new StringBuilder()
                .append("Budget: ").append(price(context.budget())).append('\n')
                .append("Intended use: ").append(context.purpose()).append('\n')
                .append("Preference in this request: ").append(orNone(context.requestPreference())).append('\n')
                .append("Saved preference: ").append(orNone(context.savedPreference())).append("\n\n")
                .append("Available products (all in stock):\n");
        for (ComponentType type : ComponentType.values()) {
            text.append(type.categoryName()).append(":\n");
            context.availableProducts().stream()
                    .filter(product -> product.componentType().orElse(null) == type)
                    .forEach(product -> text.append("- ").append(describe(product)).append('\n'));
        }
        return text.toString();
    }

    private static String describe(CatalogProduct product) {
        return "id=" + product.id() + "; name=" + product.name() + "; brand=" + product.brand()
                + "; price=" + price(product.price())
                + "; specifications: " + product.specifications().stream()
                        .map(spec -> spec.name() + "=" + spec.value())
                        .collect(Collectors.joining(", "));
    }

    private static String orNone(String value) {
        return value == null || value.isBlank() ? "none" : value;
    }

    private static String price(Float value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Answer(List<String> productIds, String description, String reasoning) {
    }
}
