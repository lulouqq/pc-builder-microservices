package com.csci318.recommendationservice.infrastructure;

import com.csci318.recommendationservice.service.AuthenticationRequiredException;
import com.csci318.recommendationservice.service.ResourceNotFoundException;
import com.csci318.recommendationservice.service.ServiceUnavailableException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Account Service GET /users/{userId}/preferences (A3), with the customer's Authorization header forwarded.
 */
class RestAccountClientTest {

    private static final String PATH = "/users/u1/preferences";

    private StubHttpServer accountService;
    private RestAccountClient client;

    @BeforeEach
    void start() throws IOException {
        accountService = new StubHttpServer();
        client = new RestAccountClient(RestClient.builder(), accountService.baseUrl(), Duration.ofMillis(500));
    }

    @AfterEach
    void stop() {
        accountService.close();
    }

    @Test
    void returnsSavedPreferenceAndForwardsAuthorizationHeader() {
        accountService.stub(PATH, 200, "{\"userId\": \"u1\", \"preference\": \"Quiet, prefers AMD\"}");

        assertThat(client.getSavedPreference("u1", "Bearer abc")).contains("Quiet, prefers AMD");
        assertThat(accountService.lastAuthorization(PATH)).isEqualTo("Bearer abc");
    }

    @Test
    void emptyWhenNoPreferenceIsSaved() {
        accountService.stub(PATH, 200, "{\"userId\": \"u1\", \"preference\": null}");

        assertThat(client.getSavedPreference("u1", "Bearer abc")).isEmpty();
    }

    @Test
    void notFoundForUnknownUser() {
        accountService.stub(PATH, 404, "{\"code\": \"ResourceNotFound\"}");

        assertThatThrownBy(() -> client.getSavedPreference("u1", "Bearer abc"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void authenticationRequiredWhenAccountRejectsTheToken() {
        accountService.stub(PATH, 401, "{\"code\": \"AuthenticationRequired\"}");

        assertThatThrownBy(() -> client.getSavedPreference("u1", "Bearer abc"))
                .isInstanceOf(AuthenticationRequiredException.class);
    }

    @Test
    void accessDeniedWhenAccountDeniesAccess() {
        accountService.stub(PATH, 403, "{\"code\": \"AccessDenied\"}");

        assertThatThrownBy(() -> client.getSavedPreference("u1", "Bearer abc"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void unavailableWhenAccountFails() {
        accountService.stub(PATH, 500, "{\"code\": \"InternalServiceError\"}");

        assertThatThrownBy(() -> client.getSavedPreference("u1", "Bearer abc"))
                .isInstanceOf(ServiceUnavailableException.class);
    }

    @Test
    void unavailableWhenAccountTimesOut() {
        accountService.stubSlow(PATH, Duration.ofSeconds(2));

        assertThatThrownBy(() -> client.getSavedPreference("u1", "Bearer abc"))
                .isInstanceOf(ServiceUnavailableException.class);
    }

    @Test
    void unavailableWhenAccountIsDown() throws IOException {
        RestAccountClient down = new RestAccountClient(RestClient.builder(), StubHttpServer.unreachableBaseUrl(),
                Duration.ofMillis(500));

        assertThatThrownBy(() -> down.getSavedPreference("u1", "Bearer abc"))
                .isInstanceOf(ServiceUnavailableException.class);
    }
}
