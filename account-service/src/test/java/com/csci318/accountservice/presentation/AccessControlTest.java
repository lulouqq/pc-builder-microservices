package com.csci318.accountservice.presentation;

import com.csci318.accountservice.domain.Role;
import com.csci318.accountservice.infrastructure.JwtTokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A2 access rules: account endpoints need a valid token (401) for the user in the path (403).
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class AccessControlTest {

    private static final String ADDRESS = """
            {"streetNo": 1, "streetName": "Main St", "suburb": "Wollongong", "city": "Wollongong",
             "state": "NSW", "postCode": 2500}
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenService tokenService;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Test
    void accountEndpointsWithoutTokenReturn401() throws Exception {
        String userId = registerUser("alice");

        for (MockHttpServletRequestBuilder request : accountRequests(userId)) {
            mockMvc.perform(request)
                    .andExpect(status().isUnauthorized())
                    .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
                    .andExpect(jsonPath("$.code").value("AuthenticationRequired"))
                    .andExpect(jsonPath("$.message").value("Authentication is required"));
        }
    }

    @Test
    void malformedTokenReturns401() throws Exception {
        String userId = registerUser("alice");

        mockMvc.perform(get("/users/{userId}", userId).header(HttpHeaders.AUTHORIZATION, "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AuthenticationRequired"))
                .andExpect(jsonPath("$.message").value("Access token is invalid or expired"));
    }

    @Test
    void expiredTokenReturns401() throws Exception {
        String userId = registerUser("alice");
        Instant issuedAt = Instant.now().minus(Duration.ofHours(2));
        String expired = encode(jwtEncoder, userId, issuedAt, issuedAt.plus(Duration.ofHours(1)));

        mockMvc.perform(get("/users/{userId}", userId).header(HttpHeaders.AUTHORIZATION, "Bearer " + expired))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AuthenticationRequired"));
    }

    @Test
    void tokenSignedWithAnotherSecretReturns401() throws Exception {
        String userId = registerUser("alice");
        JwtEncoder otherEncoder = new NimbusJwtEncoder(new ImmutableSecret<>(new SecretKeySpec(
                "another-secret-that-is-at-least-32-bytes".getBytes(StandardCharsets.UTF_8), "HmacSHA256")));
        Instant now = Instant.now();
        String forged = encode(otherEncoder, userId, now, now.plus(Duration.ofHours(1)));

        mockMvc.perform(get("/users/{userId}", userId).header(HttpHeaders.AUTHORIZATION, "Bearer " + forged))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AuthenticationRequired"));
    }

    @Test
    void basicAuthenticationIsNotAccepted() throws Exception {
        String userId = registerUser("alice");

        mockMvc.perform(get("/users/{userId}", userId).header(HttpHeaders.AUTHORIZATION, "Basic YWxpY2U6c2VjcmV0MTIz"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AuthenticationRequired"));
    }

    @Test
    void anotherUsersAccountReturns403AndIsNotChanged() throws Exception {
        String aliceId = registerUser("alice");
        String bobId = registerUser("bob");
        String aliceToken = bearer(aliceId, Role.CUSTOMER);

        for (MockHttpServletRequestBuilder request : accountRequests(bobId)) {
            mockMvc.perform(request.header(HttpHeaders.AUTHORIZATION, aliceToken))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("AccessDenied"));
        }

        mockMvc.perform(get("/users/{userId}", bobId).header(HttpHeaders.AUTHORIZATION, bearer(bobId, Role.CUSTOMER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").doesNotExist());
        mockMvc.perform(get("/users/{userId}/addresses", bobId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(bobId, Role.CUSTOMER)))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void unknownUserIdWithAnotherUsersTokenReturns403Not404() throws Exception {
        String aliceId = registerUser("alice");

        mockMvc.perform(get("/users/{userId}", "does-not-exist")
                        .header(HttpHeaders.AUTHORIZATION, bearer(aliceId, Role.CUSTOMER)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AccessDenied"));
    }

    @Test
    void adminTokenDoesNotBypassOwnUserCheck() throws Exception {
        String aliceId = registerUser("alice");

        mockMvc.perform(get("/users/{userId}", aliceId)
                        .header(HttpHeaders.AUTHORIZATION, bearer("some-admin-id", Role.ADMIN)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AccessDenied"));
    }

    @Test
    void registrationAndLoginArePublic() throws Exception {
        registerUser("alice");

        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
                        {"username": "alice", "password": "secret123"}
                        """))
                .andExpect(status().isOk());
    }

    /** One request per protected A1 endpoint for the given user. */
    private List<MockHttpServletRequestBuilder> accountRequests(String userId) {
        return List.of(
                get("/users/{userId}", userId),
                put("/users/{userId}/profile", userId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\": \"Mallory\"}"),
                get("/users/{userId}/addresses", userId),
                post("/users/{userId}/addresses", userId).contentType(MediaType.APPLICATION_JSON).content(ADDRESS),
                put("/users/{userId}/addresses/{addressId}", userId, "a1").contentType(MediaType.APPLICATION_JSON)
                        .content(ADDRESS),
                delete("/users/{userId}/addresses/{addressId}", userId, "a1"));
    }

    private String bearer(String userId, Role role) {
        return "Bearer " + tokenService.issue(userId, role).token();
    }

    private static String encode(JwtEncoder encoder, String userId, Instant issuedAt, Instant expiresAt) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(userId)
                .claim("role", Role.CUSTOMER.name())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
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
}
