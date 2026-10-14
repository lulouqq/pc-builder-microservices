package com.csci318.recommendationservice.infrastructure;

import com.csci318.recommendationservice.domain.ComponentType;
import com.csci318.recommendationservice.domain.Specification;
import com.csci318.recommendationservice.service.CatalogProduct;
import com.csci318.recommendationservice.service.ServiceUnavailableException;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Catalog Service GET /categories and GET /products, joined locally by categoryId.
 */
class RestCatalogClientTest {

    @Test
    void productsCarryTheirCategoryNameAndSpecifications() throws IOException {
        try (StubHttpServer catalogService = new StubHttpServer()) {
            catalogService.stub("/categories", 200, """
                    [{"id": "c1", "name": "CPU"}, {"id": "c2", "name": "Cooler"}]""");
            catalogService.stub("/products", 200, """
                    [{"id": "p1", "name": "Ryzen 5 7600", "price": 199.5, "brand": "AMD", "categoryId": "c1",
                      "specifications": [{"name": "Socket", "value": "AM5"}], "stockStatus": null},
                     {"id": "p2", "name": "Tower Cooler", "price": 30, "brand": "Noctua", "categoryId": "c2",
                      "specifications": []},
                     {"id": "p3", "name": "Orphan", "price": 1, "brand": "X", "categoryId": "missing",
                      "specifications": []}]""");
            RestCatalogClient client = new RestCatalogClient(RestClient.builder(), catalogService.baseUrl(),
                    Duration.ofMillis(500));

            List<CatalogProduct> products = client.getProducts();

            assertThat(products).hasSize(3);
            CatalogProduct cpu = products.get(0);
            assertThat(cpu.id()).isEqualTo("p1");
            assertThat(cpu.price()).isEqualTo(199.5f);
            assertThat(cpu.categoryName()).isEqualTo("CPU");
            assertThat(cpu.componentType()).contains(ComponentType.CPU);
            assertThat(cpu.specifications()).containsExactly(new Specification("Socket", "AM5"));
            assertThat(products.get(1).componentType()).isEmpty();
            assertThat(products.get(2).categoryName()).isNull();
        }
    }

    @Test
    void unavailableWhenCatalogFails() throws IOException {
        try (StubHttpServer catalogService = new StubHttpServer()) {
            catalogService.stub("/categories", 500, "{}");
            RestCatalogClient client = new RestCatalogClient(RestClient.builder(), catalogService.baseUrl(),
                    Duration.ofMillis(500));

            assertThatThrownBy(client::getProducts).isInstanceOf(ServiceUnavailableException.class);
        }
    }

    @Test
    void unavailableWhenCatalogIsDown() throws IOException {
        RestCatalogClient client = new RestCatalogClient(RestClient.builder(), StubHttpServer.unreachableBaseUrl(),
                Duration.ofMillis(500));

        assertThatThrownBy(client::getProducts).isInstanceOf(ServiceUnavailableException.class);
    }
}
