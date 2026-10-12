package com.csci318.catalogservice.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Aggregate root: a PC product or component available in the catalog.
 * The category is referenced by id; specifications are value objects owned by the product.
 */
@Entity
public class Product {

    @Id
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Float price;

    @Column(nullable = false)
    private String brand;

    @Column(nullable = false)
    private String categoryId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "product_specification", joinColumns = @JoinColumn(name = "product_id"))
    private List<Specification> specifications = new ArrayList<>();

    protected Product() {
    }

    public Product(String name, Float price, String brand, String categoryId, List<Specification> specifications) {
        this.id = UUID.randomUUID().toString();
        update(name, price, brand, categoryId, specifications);
    }

    public void update(String name, Float price, String brand, String categoryId, List<Specification> specifications) {
        this.name = name;
        this.price = price;
        this.brand = brand;
        this.categoryId = categoryId;
        this.specifications.clear();
        this.specifications.addAll(specifications);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Float getPrice() {
        return price;
    }

    public String getBrand() {
        return brand;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public List<Specification> getSpecifications() {
        return List.copyOf(specifications);
    }
}
