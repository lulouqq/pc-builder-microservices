package com.csci318.accountservice.presentation;

import com.csci318.accountservice.infrastructure.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserRepository userRepository;

    @Test
    void unexpectedErrorReturns500InternalServiceErrorWithoutDetails() throws Exception {
        when(userRepository.findById("u1")).thenThrow(new IllegalStateException("database password is hunter2"));

        mockMvc.perform(get("/users/{userId}", "u1"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("InternalServiceError"))
                .andExpect(jsonPath("$.message").value(not(containsString("hunter2"))));
    }

    @Test
    void unknownPathStillReturns404() throws Exception {
        mockMvc.perform(get("/does-not-exist"))
                .andExpect(status().isNotFound());
    }

    @Test
    void unsupportedMethodStillReturns405() throws Exception {
        mockMvc.perform(delete("/users/{userId}", "u1"))
                .andExpect(status().isMethodNotAllowed());
    }
}
