package com.csci318.catalogservice;

import com.csci318.catalogservice.domain.Category;
import com.csci318.catalogservice.domain.Product;
import com.csci318.catalogservice.domain.Specification;
import com.csci318.catalogservice.infrastructure.CategoryRepository;
import com.csci318.catalogservice.infrastructure.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * C1: browse and search products.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ProductBrowsingTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private String cpuCategoryId;
    private String gpuCategoryId;

    @BeforeEach
    void seedCatalog() {
        productRepository.deleteAll();
        categoryRepository.deleteAll();

        cpuCategoryId = categoryRepository.save(new Category("CPU")).getId();
        gpuCategoryId = categoryRepository.save(new Category("GPU")).getId();

        productRepository.save(new Product("Ryzen 5 7600", 299f, "AMD", cpuCategoryId,
                List.of(new Specification("Socket", "AM5"), new Specification("Cores", "6"))));
        productRepository.save(new Product("Core i5-14600K", 449f, "Intel", cpuCategoryId,
                List.of(new Specification("Socket", "LGA1700"), new Specification("Cores", "14"))));
        productRepository.save(new Product("Radeon RX 7800 XT", 799f, "AMD", gpuCategoryId,
                List.of(new Specification("Memory", "16GB"))));
    }

    @Test
    void listsAllProductsWithoutFilters() throws Exception {
        mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name")
                        .value(contains("Core i5-14600K", "Radeon RX 7800 XT", "Ryzen 5 7600")))
                .andExpect(jsonPath("$[0].price").value(449.0))
                .andExpect(jsonPath("$[0].brand").value("Intel"))
                .andExpect(jsonPath("$[0].categoryId").value(cpuCategoryId))
                .andExpect(jsonPath("$[0].specifications.length()").value(2));
    }

    @Test
    void browsesByCategory() throws Exception {
        mockMvc.perform(get("/products").param("categoryId", gpuCategoryId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name").value(contains("Radeon RX 7800 XT")));
    }

    @Test
    void filtersByBrandIgnoringCase() throws Exception {
        mockMvc.perform(get("/products").param("brand", "amd"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name").value(contains("Radeon RX 7800 XT", "Ryzen 5 7600")));
    }

    @Test
    void filtersBySpecification() throws Exception {
        mockMvc.perform(get("/products").param("specName", "Socket").param("specValue", "AM5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name").value(contains("Ryzen 5 7600")));

        // name and value must match on the same specification
        mockMvc.perform(get("/products").param("specName", "Socket").param("specValue", "6"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void combinesFiltersWithAnd() throws Exception {
        mockMvc.perform(get("/products").param("categoryId", cpuCategoryId).param("brand", "AMD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name").value(contains("Ryzen 5 7600")));

        mockMvc.perform(get("/products")
                        .param("categoryId", gpuCategoryId)
                        .param("brand", "AMD")
                        .param("specName", "Socket")
                        .param("specValue", "AM5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void rejectsSpecNameWithoutSpecValue() throws Exception {
        mockMvc.perform(get("/products").param("specName", "Socket"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ValidationFailed"));
        mockMvc.perform(get("/products").param("specValue", "AM5"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ValidationFailed"));
    }

    @Test
    void listsCategories() throws Exception {
        mockMvc.perform(get("/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name").value(contains("CPU", "GPU")))
                .andExpect(jsonPath("$[0].id").value(cpuCategoryId));
    }
}
