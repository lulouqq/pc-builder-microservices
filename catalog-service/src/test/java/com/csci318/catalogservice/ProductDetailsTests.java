package com.csci318.catalogservice;

import com.csci318.catalogservice.domain.Category;
import com.csci318.catalogservice.domain.Product;
import com.csci318.catalogservice.domain.Specification;
import com.csci318.catalogservice.domain.StockStatus;
import com.csci318.catalogservice.infrastructure.CategoryRepository;
import com.csci318.catalogservice.infrastructure.ProductRepository;
import com.csci318.catalogservice.service.InventoryClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * C2: view product details. Inventory Service is replaced by a mock of InventoryClient.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ProductDetailsTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @MockBean
    private InventoryClient inventoryClient;

    private String categoryId;
    private String productId;

    @BeforeEach
    void seedCatalog() {
        productRepository.deleteAll();
        categoryRepository.deleteAll();

        categoryId = categoryRepository.save(new Category("CPU")).getId();
        productId = productRepository.save(new Product("Ryzen 5 7600", 299f, "AMD", categoryId,
                List.of(new Specification("Socket", "AM5"), new Specification("Cores", "6")))).getId();
    }

    @Test
    void showsProductInformationSpecificationsPriceAndStockStatus() throws Exception {
        given(inventoryClient.getStockStatus(productId)).willReturn(StockStatus.IN_STOCK);

        mockMvc.perform(get("/products/{id}", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(productId))
                .andExpect(jsonPath("$.name").value("Ryzen 5 7600"))
                .andExpect(jsonPath("$.price").value(299.0))
                .andExpect(jsonPath("$.brand").value("AMD"))
                .andExpect(jsonPath("$.categoryId").value(categoryId))
                .andExpect(jsonPath("$.specifications.length()").value(2))
                .andExpect(jsonPath("$.specifications[0].name").value("Socket"))
                .andExpect(jsonPath("$.specifications[0].value").value("AM5"))
                .andExpect(jsonPath("$.stockStatus").value("IN_STOCK"));
    }

    @ParameterizedTest
    @EnumSource(StockStatus.class)
    void reportsEachStockStatusDistinctly(StockStatus stockStatus) throws Exception {
        given(inventoryClient.getStockStatus(productId)).willReturn(stockStatus);

        // UNKNOWN (no data from Inventory Service) still returns the product, and is not OUT_OF_STOCK
        mockMvc.perform(get("/products/{id}", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Ryzen 5 7600"))
                .andExpect(jsonPath("$.stockStatus").value(stockStatus.name()));
    }

    @Test
    void returnsNotFoundForUnknownProductWithoutCallingInventory() throws Exception {
        mockMvc.perform(get("/products/{id}", "unknown"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("ResourceNotFound"));

        verify(inventoryClient, never()).getStockStatus(any());
    }

    @Test
    void productListsDoNotIncludeStockStatus() throws Exception {
        mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].stockStatus").doesNotExist());

        verify(inventoryClient, never()).getStockStatus(any());
    }
}
