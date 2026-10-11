package com.csci318.catalogservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.Objects;

/**
 * Value object: a technical attribute of a product, e.g. "Socket" = "AM5".
 */
@Embeddable
public class Specification {

    // "value" is a reserved word in H2, so both columns are given explicit names
    @Column(name = "spec_name", nullable = false)
    private String name;

    @Column(name = "spec_value", nullable = false)
    private String value;

    protected Specification() {
    }

    public Specification(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Specification other)) return false;
        return Objects.equals(name, other.name) && Objects.equals(value, other.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, value);
    }
}
