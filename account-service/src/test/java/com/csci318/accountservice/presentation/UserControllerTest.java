package com.csci318.accountservice.presentation;

import com.csci318.accountservice.domain.User;
import com.csci318.accountservice.domain.UserRegistered;
import com.csci318.accountservice.infrastructure.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@RecordApplicationEvents
class UserControllerTest {

    private static final String VALID_REGISTRATION = """
            {
              "username": "alice",
              "email": "alice@example.com",
              "phone": "0400000000",
              "password": "secret123",
              "firstName": "Alice",
              "lastName": "Smith"
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ApplicationEvents events;

    @Test
    void registerReturnsCreatedUserWithoutPassword() throws Exception {
        mockMvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON).content(VALID_REGISTRATION))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.email").value("alice@example.com"))
                .andExpect(jsonPath("$.phone").value("0400000000"))
                .andExpect(jsonPath("$.firstName").value("Alice"))
                .andExpect(jsonPath("$.lastName").value("Smith"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void registerStoresBCryptHashAndPublishesUserRegistered() throws Exception {
        String userId = register(VALID_REGISTRATION);

        User stored = userRepository.findById(userId).orElseThrow();
        assertThat(stored.getPasswordHash()).isNotEqualTo("secret123").startsWith("$2");
        assertThat(passwordEncoder.matches("secret123", stored.getPasswordHash())).isTrue();

        assertThat(events.stream(UserRegistered.class))
                .singleElement()
                .satisfies(event -> {
                    assertThat(event.userId()).isEqualTo(userId);
                    assertThat(event.username()).isEqualTo("alice");
                });
    }

    @Test
    void getUserReturnsRegisteredUser() throws Exception {
        String userId = register(VALID_REGISTRATION);

        mockMvc.perform(get("/users/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.firstName").value("Alice"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void getUnknownUserReturns404() throws Exception {
        mockMvc.perform(get("/users/{userId}", "does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ResourceNotFound"));
    }

    @Test
    void registerWithMissingRequiredFieldReturns400() throws Exception {
        String missingPhone = """
                {"username": "bob", "email": "bob@example.com", "password": "secret123"}
                """;

        mockMvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON).content(missingPhone))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ValidationFailed"))
                .andExpect(jsonPath("$.message").value("phone is required"));
    }

    @Test
    void registerWithMissingPasswordReturns400() throws Exception {
        String missingPassword = """
                {"username": "bob", "email": "bob@example.com", "phone": "0400000000"}
                """;

        mockMvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON).content(missingPassword))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ValidationFailed"))
                .andExpect(jsonPath("$.message").value("password is required"));
    }

    @Test
    void registerWithDuplicateUsernameReturns400() throws Exception {
        register(VALID_REGISTRATION);

        mockMvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON).content(VALID_REGISTRATION))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ValidationFailed"))
                .andExpect(jsonPath("$.message").value("username is already taken"));
    }

    @Test
    void registerWithMalformedBodyReturns400() throws Exception {
        mockMvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ValidationFailed"));
    }

    private String register(String body) throws Exception {
        MvcResult result = mockMvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("id").asText();
    }
}
