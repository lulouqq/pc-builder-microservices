package com.csci318.recommendationservice.presentation;

import com.csci318.recommendationservice.TestCatalog;
import com.csci318.recommendationservice.domain.event.CompatibilityChecked;
import com.csci318.recommendationservice.infrastructure.BuildRequestRepository;
import com.csci318.recommendationservice.infrastructure.PCBuildRepository;
import com.csci318.recommendationservice.service.AccountClient;
import com.csci318.recommendationservice.service.CatalogClient;
import com.csci318.recommendationservice.service.InventoryClient;
import com.csci318.recommendationservice.service.ServiceUnavailableException;
import dev.langchain4j.model.chat.ChatModel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;

import static com.csci318.recommendationservice.TestCatalog.product;
import static com.csci318.recommendationservice.TestCatalog.spec;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * R2 through the REST API: public, deterministic, no LLM and nothing stored. Catalog is a mock.
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@RecordApplicationEvents
class CompatibilityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ApplicationEvents events;

    @Autowired
    private BuildRequestRepository buildRequestRepository;

    @Autowired
    private PCBuildRepository pcBuildRepository;

    @MockBean
    private CatalogClient catalogClient;

    @MockBean
    private AccountClient accountClient;

    @MockBean
    private InventoryClient inventoryClient;

    @MockBean
    private ChatModel chatModel;

    @BeforeEach
    void setUp() {
        pcBuildRepository.deleteAll();
        buildRequestRepository.deleteAll();
        when(catalogClient.getProducts()).thenReturn(TestCatalog.all());
    }

    @AfterEach
    void nothingElseIsUsedOrStored() {
        verifyNoInteractions(chatModel, accountClient, inventoryClient);
        assertThat(buildRequestRepository.count()).isZero();
        assertThat(pcBuildRepository.count()).isZero();
    }

    @Test
    void compatibleComponentsWithoutToken() throws Exception {
        validate("{\"productIds\": [\"cpu-am5\", \"mb-b650\", \"ram-ddr5\", \"case-atx\", \"gpu-4060\"]}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.compatible").value(true))
                .andExpect(jsonPath("$.conflicts", hasSize(0)));

        assertThat(events.stream(CompatibilityChecked.class)).singleElement().satisfies(event -> {
            assertThat(event.compatible()).isTrue();
            assertThat(event.productIds()).hasSize(5);
        });
    }

    @Test
    void socketMismatchIsReportedAsConflict() throws Exception {
        validate("{\"productIds\": [\"cpu-lga1700\", \"mb-b650\"]}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.compatible").value(false))
                .andExpect(jsonPath("$.conflicts", hasSize(1)))
                .andExpect(jsonPath("$.conflicts[0].specName").value("Socket"))
                .andExpect(jsonPath("$.conflicts[0].productIds", contains("cpu-lga1700", "mb-b650")))
                .andExpect(jsonPath("$.conflicts[0].message", containsString("LGA1700")));

        assertThat(events.stream(CompatibilityChecked.class)).singleElement()
                .satisfies(event -> assertThat(event.conflictCount()).isEqualTo(1));
    }

    @Test
    void missingSpecificationIsReportedAsConflict() throws Exception {
        when(catalogClient.getProducts()).thenReturn(List.of(
                product("mb", "Board", "ASUS", "Motherboard", 150f, spec("Socket", "AM5")),
                TestCatalog.RAM));

        validate("{\"productIds\": [\"mb\", \"ram-ddr5\"]}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.compatible").value(false))
                .andExpect(jsonPath("$.conflicts[0].specName").value("Memory Type"))
                .andExpect(jsonPath("$.conflicts[0].message", containsString("has no 'Memory Type' specification")));
    }

    @Test
    void ruleWithOnlyOneOfItsComponentTypesIsSkipped() throws Exception {
        validate("{\"productIds\": [\"cpu-lga1700\", \"ram-ddr5\", \"gpu-4060\"]}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.compatible").value(true));
    }

    @Test
    void valuesAreComparedTrimmedAndIgnoringCase() throws Exception {
        when(catalogClient.getProducts()).thenReturn(List.of(
                product("cpu", "CPU", "AMD", "cpu", 200f, spec("Socket", " am5 ")),
                TestCatalog.MOTHERBOARD));

        validate("{\"productIds\": [\"cpu\", \"mb-b650\"]}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.compatible").value(true));
    }

    @Test
    void emptyOrMissingProductListIsValidationFailed() throws Exception {
        validate("{\"productIds\": []}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ValidationFailed"));
        validate("{}").andExpect(status().isBadRequest());
        validate("").andExpect(status().isBadRequest());
        validate("{\"productIds\": [\" \"]}").andExpect(status().isBadRequest());
    }

    @Test
    void unknownProductIsNotFound() throws Exception {
        validate("{\"productIds\": [\"cpu-am5\", \"nope\"]}")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ResourceNotFound"))
                .andExpect(jsonPath("$.message", containsString("nope")));
    }

    @Test
    void twoProductsOfTheSameTypeAreValidationFailed() throws Exception {
        validate("{\"productIds\": [\"cpu-am5\", \"cpu-lga1700\"]}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ValidationFailed"))
                .andExpect(jsonPath("$.message", containsString("CPU")));
        validate("{\"productIds\": [\"cpu-am5\", \"cpu-am5\"]}").andExpect(status().isBadRequest());
    }

    @Test
    void productOutsideCanonicalCategoriesIsValidationFailed() throws Exception {
        validate("{\"productIds\": [\"cpu-am5\", \"cooler-1\"]}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ValidationFailed"))
                .andExpect(jsonPath("$.message", containsString("cooler-1")));
    }

    @Test
    void unavailableCatalogIsServiceUnavailable() throws Exception {
        when(catalogClient.getProducts()).thenThrow(new ServiceUnavailableException("Catalog Service is unavailable"));

        validate("{\"productIds\": [\"cpu-am5\"]}")
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("ServiceUnavailable"));
    }

    private ResultActions validate(String body) throws Exception {
        return mockMvc.perform(post("/compatibility/validate").contentType(MediaType.APPLICATION_JSON).content(body));
    }
}
