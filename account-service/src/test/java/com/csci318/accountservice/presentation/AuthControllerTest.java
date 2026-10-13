package com.csci318.accountservice.presentation;

import com.csci318.accountservice.domain.Role;
import com.csci318.accountservice.domain.User;
import com.csci318.accountservice.infrastructure.AdminAccountInitializer;
import com.csci318.accountservice.infrastructure.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class AuthControllerTest {

    // Admin credentials from src/test/resources/application-test.yml.
    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD = "test-admin-password";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtDecoder jwtDecoder;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AdminAccountInitializer adminAccountInitializer;

    @Test
    void loginReturnsBearerTokenForTheUser() throws Exception {
        String userId = register("alice");

        JsonNode response = json(login("alice", "secret123")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.role").value("CUSTOMER"))
                .andReturn());

        Jwt jwt = jwtDecoder.decode(response.get("token").asText());
        assertThat(jwt.getSubject()).isEqualTo(userId);
        assertThat(jwt.getClaimAsString("role")).isEqualTo("CUSTOMER");
        assertThat(Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt())).isEqualTo(Duration.ofHours(1));
        assertThat(Instant.parse(response.get("expiresAt").asText())).isEqualTo(jwt.getExpiresAt());
    }

    @Test
    void tokenFromLoginGivesAccessToOwnAccount() throws Exception {
        String userId = register("alice");
        String token = json(login("alice", "secret123").andReturn()).get("token").asText();

        mockMvc.perform(get("/users/{userId}", userId).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("alice"));
    }

    @Test
    void wrongPasswordReturns401() throws Exception {
        register("alice");

        login("alice", "wrong-password")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AuthenticationRequired"))
                .andExpect(jsonPath("$.message").value("Invalid username or password"))
                .andExpect(jsonPath("$.token").doesNotExist());
    }

    @Test
    void unknownUsernameReturns401WithSameMessage() throws Exception {
        login("nobody", "secret123")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AuthenticationRequired"))
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }

    @Test
    void missingCredentialsReturn400() throws Exception {
        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
                        {"username": "alice"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ValidationFailed"))
                .andExpect(jsonPath("$.message").value("password is required"));
        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
                        {"username": " ", "password": "secret123"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("username is required"));
        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ValidationFailed"));
    }

    @Test
    void seededAdminCanLogInWithAdminRole() throws Exception {
        User admin = userRepository.findByUsername(ADMIN_USERNAME).orElseThrow();
        assertThat(admin.getRole()).isEqualTo(Role.ADMIN);

        JsonNode response = json(login(ADMIN_USERNAME, ADMIN_PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(admin.getId()))
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andReturn());
        assertThat(jwtDecoder.decode(response.get("token").asText()).getClaimAsString("role")).isEqualTo("ADMIN");
    }

    @Test
    void adminIsNotCreatedTwice() throws Exception {
        long users = userRepository.count();

        adminAccountInitializer.run(null);

        assertThat(userRepository.count()).isEqualTo(users);
    }

    @Test
    void registrationAlwaysCreatesCustomer() throws Exception {
        mockMvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON).content("""
                        {"username": "mallory", "email": "m@example.com", "phone": "0400000000",
                         "password": "secret123", "role": "ADMIN"}
                        """))
                .andExpect(status().isCreated());

        assertThat(userRepository.findByUsername("mallory").orElseThrow().getRole()).isEqualTo(Role.CUSTOMER);
    }

    private ResultActions login(String username, String password) throws Exception {
        return mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
                {"username": "%s", "password": "%s"}
                """.formatted(username, password)));
    }

    private String register(String username) throws Exception {
        String body = """
                {"username": "%s", "email": "%s@example.com", "phone": "0400000000", "password": "secret123"}
                """.formatted(username, username);
        return json(mockMvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn()).get("id").asText();
    }

    private JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
