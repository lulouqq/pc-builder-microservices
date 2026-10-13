package com.csci318.accountservice.presentation;

import com.csci318.accountservice.domain.PreferenceUpdated;
import com.csci318.accountservice.domain.Role;
import com.csci318.accountservice.infrastructure.JwtTokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
@RecordApplicationEvents
class PreferenceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenService tokenService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private ApplicationEvents events;

    @Test
    void getPreferenceBeforeAnyIsSavedReturnsNull() throws Exception {
        String userId = registerUser("alice");

        getPreference(userId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$", hasKey("preference")))
                .andExpect(jsonPath("$.preference").value(nullValue()));
    }

    @Test
    void putPreferenceSavesItAndPublishesPreferenceUpdated() throws Exception {
        String userId = registerUser("alice");

        putPreference(userId, "{\"preference\": \"Quiet gaming PC, prefers AMD\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.preference").value("Quiet gaming PC, prefers AMD"));

        assertThat(events.stream(PreferenceUpdated.class))
                .singleElement()
                .satisfies(event -> {
                    assertThat(event.userId()).isEqualTo(userId);
                    assertThat(event.preference()).isEqualTo("Quiet gaming PC, prefers AMD");
                    assertThat(event.occurredAt()).isNotNull();
                });

        entityManager.flush();
        entityManager.clear();
        getPreference(userId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.preference").value("Quiet gaming PC, prefers AMD"));
    }

    @Test
    void putPreferenceReplacesSavedPreference() throws Exception {
        String userId = registerUser("alice");
        putPreference(userId, "{\"preference\": \"Gaming\"}").andExpect(status().isOk());

        putPreference(userId, "{\"preference\": \"Video editing\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.preference").value("Video editing"));

        assertThat(events.stream(PreferenceUpdated.class)).hasSize(2);
    }

    @Test
    void longPreferenceIsStoredWithoutLimit() throws Exception {
        String userId = registerUser("alice");
        String longText = "Silent build with RGB lighting. ".repeat(500);

        putPreference(userId, objectMapper.writeValueAsString(Map.of("preference", longText)))
                .andExpect(status().isOk());
        entityManager.flush();
        entityManager.clear();

        getPreference(userId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.preference").value(longText));
    }

    @Test
    void putSamePreferenceDoesNotPublishPreferenceUpdated() throws Exception {
        String userId = registerUser("alice");
        putPreference(userId, "{\"preference\": \"Gaming\"}").andExpect(status().isOk());

        putPreference(userId, "{\"preference\": \"Gaming\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.preference").value("Gaming"));

        assertThat(events.stream(PreferenceUpdated.class)).hasSize(1);
    }

    @Test
    void missingNullOrBlankPreferenceClearsIt() throws Exception {
        String userId = registerUser("alice");

        for (String clearingBody : new String[] {"{}", "{\"preference\": null}", "{\"preference\": \"   \"}"}) {
            putPreference(userId, "{\"preference\": \"Gaming\"}").andExpect(status().isOk());

            putPreference(userId, clearingBody)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasKey("preference")))
                    .andExpect(jsonPath("$.preference").value(nullValue()));
            entityManager.flush();
            entityManager.clear();
            getPreference(userId).andExpect(jsonPath("$.preference").value(nullValue()));
        }

        // Each round: one event for setting "Gaming", one for clearing it.
        assertThat(events.stream(PreferenceUpdated.class)).hasSize(6);
        assertThat(events.stream(PreferenceUpdated.class).filter(event -> event.preference() == null)).hasSize(3);
    }

    @Test
    void clearingWhenNoPreferenceIsSavedDoesNotPublishPreferenceUpdated() throws Exception {
        String userId = registerUser("alice");

        putPreference(userId, "{\"preference\": \" \"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.preference").value(nullValue()));

        assertThat(events.stream(PreferenceUpdated.class)).isEmpty();
    }

    @Test
    void preferenceIsNotPartOfUserResponse() throws Exception {
        String userId = registerUser("alice");
        putPreference(userId, "{\"preference\": \"Gaming\"}").andExpect(status().isOk());

        mockMvc.perform(get("/users/{userId}", userId).header(HttpHeaders.AUTHORIZATION, bearer(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.preference").doesNotExist());
    }

    @Test
    void preferenceOfUnknownUserReturns404() throws Exception {
        getPreference("does-not-exist")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ResourceNotFound"));
        putPreference("does-not-exist", "{\"preference\": \"Gaming\"}")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ResourceNotFound"));
    }

    @Test
    void malformedBodyReturns400() throws Exception {
        String userId = registerUser("alice");

        putPreference(userId, "{\"preference\": ")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ValidationFailed"));
    }

    private ResultActions getPreference(String userId) throws Exception {
        return mockMvc.perform(get("/users/{userId}/preferences", userId)
                .header(HttpHeaders.AUTHORIZATION, bearer(userId)));
    }

    private ResultActions putPreference(String userId, String body) throws Exception {
        return mockMvc.perform(put("/users/{userId}/preferences", userId)
                .header(HttpHeaders.AUTHORIZATION, bearer(userId))
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private String registerUser(String username) throws Exception {
        String body = """
                {"username": "%s", "email": "%s@example.com", "phone": "0400000000", "password": "secret123"}
                """.formatted(username, username);
        String response = mockMvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asText();
    }

    private String bearer(String userId) {
        return "Bearer " + tokenService.issue(userId, Role.CUSTOMER).token();
    }
}
