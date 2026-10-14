package com.csci318.recommendationservice.presentation;

import com.csci318.recommendationservice.TestCatalog;
import com.csci318.recommendationservice.domain.event.BuildRequested;
import com.csci318.recommendationservice.domain.event.RecommendationGenerated;
import com.csci318.recommendationservice.infrastructure.BuildRequestRepository;
import com.csci318.recommendationservice.infrastructure.PCBuildRepository;
import com.csci318.recommendationservice.service.AccountClient;
import com.csci318.recommendationservice.service.AuthenticationRequiredException;
import com.csci318.recommendationservice.service.CatalogClient;
import com.csci318.recommendationservice.service.InventoryClient;
import com.csci318.recommendationservice.service.ResourceNotFoundException;
import com.csci318.recommendationservice.service.ServiceUnavailableException;
import com.jayway.jsonpath.JsonPath;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;
import java.util.Optional;

import static com.csci318.recommendationservice.TestTokens.bearer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * R1, R3 and R4 through the REST API. Account, Catalog and Inventory clients and the LangChain4j chat model
 * are mocks; everything else (security, services, validation, H2 persistence) is real.
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@RecordApplicationEvents
class RecommendationControllerTest {

    private static final String ALICE = "alice";
    private static final String BOB = "bob";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ApplicationEvents events;

    @Autowired
    private BuildRequestRepository buildRequestRepository;

    @Autowired
    private PCBuildRepository pcBuildRepository;

    @MockBean
    private AccountClient accountClient;

    @MockBean
    private CatalogClient catalogClient;

    @MockBean
    private InventoryClient inventoryClient;

    @MockBean
    private ChatModel chatModel;

    /** The Authorization header sent by the last {@link #generate} call. */
    private String lastBearer;

    @BeforeEach
    void setUp() {
        pcBuildRepository.deleteAll();
        buildRequestRepository.deleteAll();
        when(accountClient.getSavedPreference(anyString(), anyString())).thenReturn(Optional.of("Quiet PC"));
        when(catalogClient.getProducts()).thenReturn(TestCatalog.all());
        when(inventoryClient.isInStock(anyString())).thenReturn(true);
    }

    // ---- R1: POST /recommendations ----

    @Test
    void generatesValidBuildStoresItAndPublishesEvents() throws Exception {
        when(chatModel.chat(anyList())).thenReturn(answer(TestCatalog.VALID_BUILD));

        generate(ALICE, request(ALICE, 1500, "gaming", "Prefer AMD"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.recommendationId", notNullValue()))
                .andExpect(jsonPath("$.buildId", notNullValue()))
                .andExpect(jsonPath("$.userId").value(ALICE))
                .andExpect(jsonPath("$.budget").value(1500.0))
                .andExpect(jsonPath("$.purpose").value("gaming"))
                .andExpect(jsonPath("$.preferences").value("Prefer AMD"))
                .andExpect(jsonPath("$.totalPrice").value(990.0))
                .andExpect(jsonPath("$.description").value("Balanced AM5 build"))
                .andExpect(jsonPath("$.reasoning").value("Fits the budget and the preferences"))
                .andExpect(jsonPath("$.components", hasSize(7)))
                .andExpect(jsonPath("$.components[0].productId").value("cpu-am5"))
                .andExpect(jsonPath("$.components[0].componentType").value("CPU"))
                .andExpect(jsonPath("$.components[0].price").value(200.0))
                .andExpect(jsonPath("$.components[1].componentType").value("Motherboard"));

        verify(accountClient).getSavedPreference(ALICE, lastBearer);
        String prompt = userText(sentMessages(1).get(0).get(1));
        assertThat(prompt)
                .contains("Preference in this request: Prefer AMD")
                .contains("Saved preference: Quiet PC")
                .contains("id=cpu-am5")
                .doesNotContain("cooler-1");

        assertThat(buildRequestRepository.count()).isEqualTo(1);
        assertThat(pcBuildRepository.count()).isEqualTo(1);
        assertThat(events.stream(BuildRequested.class)).singleElement()
                .satisfies(event -> assertThat(event.userId()).isEqualTo(ALICE));
        assertThat(events.stream(RecommendationGenerated.class)).singleElement()
                .satisfies(event -> assertThat(event.totalPrice()).isEqualTo(990f));
    }

    @Test
    void savedPreferenceIsOnlyContextAndNotStored() throws Exception {
        when(chatModel.chat(anyList())).thenReturn(answer(TestCatalog.VALID_BUILD));

        generate(ALICE, "{\"userId\": \"alice\", \"budget\": 1500, \"purpose\": \"office\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.preferences", nullValue()));

        assertThat(userText(sentMessages(1).get(0).get(1)))
                .contains("Preference in this request: none")
                .contains("Saved preference: Quiet PC");
    }

    @Test
    void invalidAnswerIsRetriedOnceWithValidationFeedback() throws Exception {
        List<String> wrongSocket = replace(TestCatalog.VALID_BUILD, "cpu-am5", "cpu-lga1700");
        when(chatModel.chat(anyList())).thenReturn(answer(wrongSocket), answer(TestCatalog.VALID_BUILD));

        generate(ALICE, request(ALICE, 1500, "gaming", null))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.components[0].productId").value("cpu-am5"));

        List<ChatMessage> retry = sentMessages(2).get(1);
        assertThat(retry).hasSize(4);
        assertThat(((AiMessage) retry.get(2)).text()).contains("cpu-lga1700");
        assertThat(userText(retry.get(3))).contains("Socket does not match");
    }

    @Test
    void unreadableAnswerIsRetried() throws Exception {
        when(chatModel.chat(anyList())).thenReturn(answer("I would suggest an AMD build."),
                answer(TestCatalog.VALID_BUILD));

        generate(ALICE, request(ALICE, 1500, "gaming", null)).andExpect(status().isCreated());

        assertThat(userText(sentMessages(2).get(1).get(3))).contains("not a JSON object");
    }

    @Test
    void answerOverBudgetIsRejected() throws Exception {
        // budget 980: the valid build costs 990, with the cheaper GPU it costs 970
        List<String> cheaper = replace(TestCatalog.VALID_BUILD, "gpu-4060", "gpu-7600");
        when(chatModel.chat(anyList())).thenReturn(answer(TestCatalog.VALID_BUILD), answer(cheaper));

        generate(ALICE, request(ALICE, 980, "gaming", null))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalPrice").value(970.0));

        assertThat(userText(sentMessages(2).get(1).get(3))).contains("total price 990.00 is over the budget 980.00");
    }

    @Test
    void outOfStockProductsAreNotOfferedOrAccepted() throws Exception {
        when(inventoryClient.isInStock("gpu-4060")).thenReturn(false);
        List<String> cheaper = replace(TestCatalog.VALID_BUILD, "gpu-4060", "gpu-7600");
        when(chatModel.chat(anyList())).thenReturn(answer(TestCatalog.VALID_BUILD), answer(cheaper));

        generate(ALICE, request(ALICE, 1500, "gaming", null)).andExpect(status().isCreated());

        List<List<ChatMessage>> calls = sentMessages(2);
        assertThat(userText(calls.get(0).get(1))).doesNotContain("gpu-4060").contains("gpu-7600");
        assertThat(userText(calls.get(1).get(3))).contains("Product gpu-4060 is not one of the available products");
    }

    @Test
    void secondInvalidAnswerReturnsCompatibilityConflictAndStoresNothing() throws Exception {
        List<String> unknownProduct = replace(TestCatalog.VALID_BUILD, "case-atx", "not-a-product");
        when(chatModel.chat(anyList())).thenReturn(answer(unknownProduct));

        generate(ALICE, request(ALICE, 1500, "gaming", null))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CompatibilityConflict"))
                .andExpect(jsonPath("$.message", containsString("not-a-product")));

        verify(chatModel, times(2)).chat(anyList());
        assertThat(buildRequestRepository.count()).isZero();
        assertThat(pcBuildRepository.count()).isZero();
        assertThat(events.stream(RecommendationGenerated.class)).isEmpty();
    }

    @Test
    void missingComponentTypeInStockReturnsConflictWithoutAskingTheLlm() throws Exception {
        when(inventoryClient.isInStock("psu-750")).thenReturn(false);

        generate(ALICE, request(ALICE, 1500, "gaming", null))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CompatibilityConflict"))
                .andExpect(jsonPath("$.message", containsString("PSU")));

        verifyNoInteractions(chatModel);
    }

    @Test
    void budgetBelowCheapestBuildReturnsConflictWithoutAskingTheLlm() throws Exception {
        generate(ALICE, request(ALICE, 500, "gaming", null))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CompatibilityConflict"))
                .andExpect(jsonPath("$.message", containsString("960.00")));

        verifyNoInteractions(chatModel);
    }

    @Test
    void llmFailureReturnsServiceUnavailable() throws Exception {
        when(chatModel.chat(anyList())).thenThrow(new RuntimeException("Read timed out"));

        generate(ALICE, request(ALICE, 1500, "gaming", null))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("ServiceUnavailable"));

        assertThat(buildRequestRepository.count()).isZero();
    }

    @Test
    void llmFailureOnRetryReturnsServiceUnavailable() throws Exception {
        when(chatModel.chat(anyList()))
                .thenReturn(answer("not json"))
                .thenThrow(new RuntimeException("Read timed out"));

        generate(ALICE, request(ALICE, 1500, "gaming", null))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("ServiceUnavailable"));
    }

    @Test
    void unavailableDownstreamServiceReturnsServiceUnavailable() throws Exception {
        when(accountClient.getSavedPreference(anyString(), anyString()))
                .thenThrow(new ServiceUnavailableException("Account Service is unavailable"));
        generate(ALICE, request(ALICE, 1500, "gaming", null))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value("Account Service is unavailable"));

        doReturn(Optional.empty()).when(accountClient).getSavedPreference(anyString(), anyString());
        doThrow(new ServiceUnavailableException("Catalog Service is unavailable")).when(catalogClient).getProducts();
        generate(ALICE, request(ALICE, 1500, "gaming", null))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value("Catalog Service is unavailable"));

        doReturn(TestCatalog.all()).when(catalogClient).getProducts();
        doThrow(new ServiceUnavailableException("Inventory Service is unavailable"))
                .when(inventoryClient).isInStock(anyString());
        generate(ALICE, request(ALICE, 1500, "gaming", null))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value("Inventory Service is unavailable"));

        verifyNoInteractions(chatModel);
    }

    @Test
    void accountErrorsArePassedOn() throws Exception {
        when(accountClient.getSavedPreference(anyString(), anyString()))
                .thenThrow(new AuthenticationRequiredException("Account Service did not accept the access token"));
        generate(ALICE, request(ALICE, 1500, "gaming", null))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AuthenticationRequired"));

        doThrow(new AccessDeniedException("Account Service denied access to the user's preferences"))
                .when(accountClient).getSavedPreference(anyString(), anyString());
        generate(ALICE, request(ALICE, 1500, "gaming", null))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AccessDenied"));

        doThrow(new ResourceNotFoundException("User not found: alice"))
                .when(accountClient).getSavedPreference(anyString(), anyString());
        generate(ALICE, request(ALICE, 1500, "gaming", null))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ResourceNotFound"));

        verifyNoInteractions(chatModel);
        assertThat(buildRequestRepository.count()).isZero();
    }

    @Test
    void invalidRequestReturnsValidationFailed() throws Exception {
        generate(ALICE, request(ALICE, 0, "gaming", null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ValidationFailed"))
                .andExpect(jsonPath("$.message", containsString("budget")));
        generate(ALICE, request(ALICE, -10, "gaming", null)).andExpect(status().isBadRequest());
        generate(ALICE, "{\"userId\": \"alice\", \"purpose\": \"gaming\"}").andExpect(status().isBadRequest());
        generate(ALICE, request(ALICE, 1500, " ", null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("purpose")));
        generate(ALICE, "{\"budget\": 1500, \"purpose\": \"gaming\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("userId")));
        generate(ALICE, "not json").andExpect(status().isBadRequest());

        verifyNoInteractions(chatModel, accountClient);
    }

    @Test
    void generatingRequiresAValidToken() throws Exception {
        mockMvc.perform(post("/recommendations").contentType(MediaType.APPLICATION_JSON)
                        .content(request(ALICE, 1500, "gaming", null)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AuthenticationRequired"));
        mockMvc.perform(post("/recommendations").contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION,
                                bearer(ALICE, "another-secret-that-is-at-least-32-bytes-long"))
                        .content(request(ALICE, 1500, "gaming", null)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void generatingForAnotherUserIsForbidden() throws Exception {
        generate(BOB, request(ALICE, 1500, "gaming", null))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AccessDenied"));

        verifyNoInteractions(accountClient, chatModel);
    }

    // ---- R3: GET /recommendations/{recommendationId} ----

    @Test
    void ownerCanViewRecommendation() throws Exception {
        String recommendationId = generateValid(ALICE);

        mockMvc.perform(get("/recommendations/{id}", recommendationId).header(HttpHeaders.AUTHORIZATION, bearer(ALICE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recommendationId").value(recommendationId))
                .andExpect(jsonPath("$.userId").value(ALICE))
                .andExpect(jsonPath("$.totalPrice").value(990.0))
                .andExpect(jsonPath("$.reasoning").value("Fits the budget and the preferences"))
                .andExpect(jsonPath("$.components", hasSize(7)))
                .andExpect(jsonPath("$.components[6].componentType").value("Case"))
                .andExpect(jsonPath("$.components[6].price").value(70.0));
    }

    @Test
    void anotherUsersRecommendationIsForbiddenAndUnknownIsNotFound() throws Exception {
        String recommendationId = generateValid(ALICE);

        mockMvc.perform(get("/recommendations/{id}", recommendationId).header(HttpHeaders.AUTHORIZATION, bearer(BOB)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AccessDenied"));
        mockMvc.perform(get("/recommendations/{id}", "unknown").header(HttpHeaders.AUTHORIZATION, bearer(BOB)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ResourceNotFound"));
        mockMvc.perform(get("/recommendations/{id}", recommendationId))
                .andExpect(status().isUnauthorized());
    }

    // ---- R4: GET /users/{userId}/recommendations ----

    @Test
    void customerSeesOnlyOwnPreviousRecommendations() throws Exception {
        generateValid(ALICE);
        generateValid(ALICE);
        generateValid(BOB);

        mockMvc.perform(get("/users/{userId}/recommendations", ALICE).header(HttpHeaders.AUTHORIZATION, bearer(ALICE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].userId", everyItem(is(ALICE))))
                .andExpect(jsonPath("$[0].purpose").value("gaming"))
                .andExpect(jsonPath("$[0].components", hasSize(7)));
        mockMvc.perform(get("/users/{userId}/recommendations", "carol").header(HttpHeaders.AUTHORIZATION, bearer("carol")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void anotherUsersRecommendationListIsForbidden() throws Exception {
        mockMvc.perform(get("/users/{userId}/recommendations", ALICE).header(HttpHeaders.AUTHORIZATION, bearer(BOB)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AccessDenied"));
        mockMvc.perform(get("/users/{userId}/recommendations", ALICE))
                .andExpect(status().isUnauthorized());
    }

    // ---- helpers ----

    private ResultActions generate(String tokenUser, String body) throws Exception {
        lastBearer = bearer(tokenUser);
        return mockMvc.perform(post("/recommendations")
                .header(HttpHeaders.AUTHORIZATION, lastBearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private String generateValid(String userId) throws Exception {
        when(chatModel.chat(anyList())).thenReturn(answer(TestCatalog.VALID_BUILD));
        String body = generate(userId, request(userId, 1500, "gaming", null))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.recommendationId");
    }

    private static String request(String userId, double budget, String purpose, String preferences) {
        return "{\"userId\": \"" + userId + "\", \"budget\": " + budget + ", \"purpose\": \"" + purpose + "\""
                + (preferences == null ? "" : ", \"preferences\": \"" + preferences + "\"") + "}";
    }

    private static ChatResponse answer(List<String> productIds) {
        String ids = String.join("\", \"", productIds);
        return answer("{\"productIds\": [\"" + ids + "\"], \"description\": \"Balanced AM5 build\", "
                + "\"reasoning\": \"Fits the budget and the preferences\"}");
    }

    private static ChatResponse answer(String text) {
        return ChatResponse.builder().aiMessage(AiMessage.from(text)).build();
    }

    private static List<String> replace(List<String> ids, String from, String to) {
        return ids.stream().map(id -> id.equals(from) ? to : id).toList();
    }

    @SuppressWarnings("unchecked")
    private List<List<ChatMessage>> sentMessages(int calls) {
        ArgumentCaptor<List<ChatMessage>> captor = ArgumentCaptor.forClass(List.class);
        verify(chatModel, times(calls)).chat(captor.capture());
        return captor.getAllValues();
    }

    private static String userText(ChatMessage message) {
        return ((UserMessage) message).singleText();
    }
}
