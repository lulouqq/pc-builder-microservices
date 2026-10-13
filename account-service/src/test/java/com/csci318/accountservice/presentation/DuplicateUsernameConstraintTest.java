package com.csci318.accountservice.presentation;

import com.csci318.accountservice.domain.UserRegistered;
import com.csci318.accountservice.infrastructure.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Simulates two concurrent registrations with the same username: both pass the existsByUsername check,
 * so the second one is rejected by the database unique constraint.
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
@RecordApplicationEvents
class DuplicateUsernameConstraintTest {

    private static final String REGISTRATION = """
            {
              "username": "carol",
              "email": "carol@example.com",
              "phone": "0400000000",
              "password": "secret123"
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ApplicationEvents events;

    @SpyBean
    private UserRepository userRepository;

    @Test
    void uniqueConstraintViolationReturns400ValidationFailed() throws Exception {
        doReturn(false).when(userRepository).existsByUsername(anyString());

        mockMvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON).content(REGISTRATION))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON).content(REGISTRATION))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ValidationFailed"))
                .andExpect(jsonPath("$.message").value("username is already taken"));

        assertThat(events.stream(UserRegistered.class)).hasSize(1);
    }
}
