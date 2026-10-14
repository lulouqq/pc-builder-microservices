package com.csci318.recommendationservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;

import java.util.UUID;

/**
 * Entity inside the PCBuild aggregate: one product of the build, with its price when the build was generated.
 */
@Entity
public class BuildComponent {

    @Id
    private String id;

    @Column(nullable = false)
    private String productId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ComponentType componentType;

    @Column(nullable = false)
    private Float price;

    protected BuildComponent() {
    }

    public BuildComponent(String productId, ComponentType componentType, Float price) {
        if (productId == null || productId.isBlank() || componentType == null || price == null || price < 0) {
            throw new IllegalArgumentException("A build component needs a productId, a componentType and a price");
        }
        this.id = UUID.randomUUID().toString();
        this.productId = productId;
        this.componentType = componentType;
        this.price = price;
    }

    public String getId() {
        return id;
    }

    public String getProductId() {
        return productId;
    }

    public ComponentType getComponentType() {
        return componentType;
    }

    public Float getPrice() {
        return price;
    }
}
