package com.csci318.catalogservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.util.UUID;

/**
 * Aggregate root: a product category used to organise related PC components.
 */
@Entity
public class Category {

    @Id
    private String id;

    @Column(nullable = false)
    private String name;

    protected Category() {
    }

    public Category(String name) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
    }

    public void rename(String name) {
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
