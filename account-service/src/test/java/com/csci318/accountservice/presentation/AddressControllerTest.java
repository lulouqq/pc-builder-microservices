package com.csci318.accountservice.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AddressControllerTest {

    private static final String ADDRESS = """
            {
              "streetNo": 1,
              "streetName": "Main St",
              "suburb": "Wollongong",
              "city": "Wollongong",
              "state": "NSW",
              "postCode": 2500
            }
            """;

    private static final String UPDATED_ADDRESS = """
            {
              "streetNo": 2,
              "streetName": "High St",
              "suburb": "Fairy Meadow",
              "city": "Wollongong",
              "state": "NSW",
              "postCode": 2519
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EntityManager entityManager;

    @Test
    void addAddressReturnsCreatedAddress() throws Exception {
        String userId = registerUser("alice");

        mockMvc.perform(post("/users/{userId}/addresses", userId).contentType(MediaType.APPLICATION_JSON)
                        .content(ADDRESS))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.streetNo").value(1))
                .andExpect(jsonPath("$.streetName").value("Main St"))
                .andExpect(jsonPath("$.suburb").value("Wollongong"))
                .andExpect(jsonPath("$.city").value("Wollongong"))
                .andExpect(jsonPath("$.state").value("NSW"))
                .andExpect(jsonPath("$.postCode").value(2500));
    }

    @Test
    void getAddressesReturnsSavedAddressesFromDatabase() throws Exception {
        String userId = registerUser("alice");
        String addressId = addAddress(userId, ADDRESS);
        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(get("/users/{userId}/addresses", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(addressId))
                .andExpect(jsonPath("$[0].streetName").value("Main St"))
                .andExpect(jsonPath("$[0].postCode").value(2500));
    }

    @Test
    void getAddressesOfUserWithoutAddressesReturnsEmptyArray() throws Exception {
        String userId = registerUser("alice");

        mockMvc.perform(get("/users/{userId}/addresses", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void updateAddressReturnsUpdatedAddress() throws Exception {
        String userId = registerUser("alice");
        String addressId = addAddress(userId, ADDRESS);

        mockMvc.perform(put("/users/{userId}/addresses/{addressId}", userId, addressId)
                        .contentType(MediaType.APPLICATION_JSON).content(UPDATED_ADDRESS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(addressId))
                .andExpect(jsonPath("$.streetNo").value(2))
                .andExpect(jsonPath("$.streetName").value("High St"))
                .andExpect(jsonPath("$.suburb").value("Fairy Meadow"))
                .andExpect(jsonPath("$.postCode").value(2519));
        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(get("/users/{userId}/addresses", userId))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].streetName").value("High St"));
    }

    @Test
    void removeAddressReturns204AndDeletesIt() throws Exception {
        String userId = registerUser("alice");
        String addressId = addAddress(userId, ADDRESS);

        mockMvc.perform(delete("/users/{userId}/addresses/{addressId}", userId, addressId))
                .andExpect(status().isNoContent());
        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(get("/users/{userId}/addresses", userId))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void addAddressWithMissingFieldReturns400() throws Exception {
        String userId = registerUser("alice");
        String missingPostCode = """
                {"streetNo": 1, "streetName": "Main St", "suburb": "Wollongong", "city": "Wollongong", "state": "NSW"}
                """;

        mockMvc.perform(post("/users/{userId}/addresses", userId).contentType(MediaType.APPLICATION_JSON)
                        .content(missingPostCode))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ValidationFailed"))
                .andExpect(jsonPath("$.message").value("postCode is required"));
    }

    @Test
    void addAddressWithNonIntegerPostCodeReturns400() throws Exception {
        String userId = registerUser("alice");
        String badPostCode = ADDRESS.replace("2500", "\"abc\"");

        mockMvc.perform(post("/users/{userId}/addresses", userId).contentType(MediaType.APPLICATION_JSON)
                        .content(badPostCode))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ValidationFailed"));
    }

    @Test
    void updateAddressWithMissingFieldReturns400() throws Exception {
        String userId = registerUser("alice");
        String addressId = addAddress(userId, ADDRESS);
        String missingCity = UPDATED_ADDRESS.replace("\"city\": \"Wollongong\",", "");

        mockMvc.perform(put("/users/{userId}/addresses/{addressId}", userId, addressId)
                        .contentType(MediaType.APPLICATION_JSON).content(missingCity))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ValidationFailed"))
                .andExpect(jsonPath("$.message").value("city is required"));
    }

    @Test
    void addressEndpointsOfUnknownUserReturn404() throws Exception {
        mockMvc.perform(get("/users/{userId}/addresses", "does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ResourceNotFound"));
        mockMvc.perform(post("/users/{userId}/addresses", "does-not-exist").contentType(MediaType.APPLICATION_JSON)
                        .content(ADDRESS))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ResourceNotFound"));
    }

    @Test
    void unknownAddressReturns404() throws Exception {
        String userId = registerUser("alice");

        mockMvc.perform(put("/users/{userId}/addresses/{addressId}", userId, "does-not-exist")
                        .contentType(MediaType.APPLICATION_JSON).content(UPDATED_ADDRESS))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ResourceNotFound"));
        mockMvc.perform(delete("/users/{userId}/addresses/{addressId}", userId, "does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ResourceNotFound"));
    }

    @Test
    void addressOfAnotherUserReturns404AndIsNotChanged() throws Exception {
        String aliceId = registerUser("alice");
        String bobId = registerUser("bob");
        String aliceAddressId = addAddress(aliceId, ADDRESS);

        mockMvc.perform(put("/users/{userId}/addresses/{addressId}", bobId, aliceAddressId)
                        .contentType(MediaType.APPLICATION_JSON).content(UPDATED_ADDRESS))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/users/{userId}/addresses/{addressId}", bobId, aliceAddressId))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/users/{userId}/addresses", aliceId))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].streetName").value("Main St"));
    }

    private String registerUser(String username) throws Exception {
        String body = """
                {"username": "%s", "email": "%s@example.com", "phone": "0400000000", "password": "secret123"}
                """.formatted(username, username);
        return idOf(mockMvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn());
    }

    private String addAddress(String userId, String body) throws Exception {
        return idOf(mockMvc.perform(post("/users/{userId}/addresses", userId)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn());
    }

    private String idOf(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }
}
