package com.csci318.accountservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * Entity inside the User aggregate: a saved customer delivery address (all fields required).
 * Created, updated and removed only through {@link User}.
 */
@Entity
@Table(name = "addresses")
public class Address {

    @Id
    private String id;

    @Column(nullable = false)
    private Integer streetNo;

    @Column(nullable = false)
    private String streetName;

    @Column(nullable = false)
    private String suburb;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private String state;

    @Column(nullable = false)
    private Integer postCode;

    protected Address() {
        // for JPA
    }

    Address(Integer streetNo, String streetName, String suburb, String city, String state, Integer postCode) {
        this.id = UUID.randomUUID().toString();
        update(streetNo, streetName, suburb, city, state, postCode);
    }

    void update(Integer streetNo, String streetName, String suburb, String city, String state, Integer postCode) {
        requireValue(streetNo, "streetNo");
        requireText(streetName, "streetName");
        requireText(suburb, "suburb");
        requireText(city, "city");
        requireText(state, "state");
        requireValue(postCode, "postCode");

        this.streetNo = streetNo;
        this.streetName = streetName;
        this.suburb = suburb;
        this.city = city;
        this.state = state;
        this.postCode = postCode;
    }

    private static void requireValue(Integer value, String field) {
        if (value == null) {
            throw new ValidationException(field + " is required");
        }
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new ValidationException(field + " is required");
        }
    }

    public String getId() {
        return id;
    }

    public Integer getStreetNo() {
        return streetNo;
    }

    public String getStreetName() {
        return streetName;
    }

    public String getSuburb() {
        return suburb;
    }

    public String getCity() {
        return city;
    }

    public String getState() {
        return state;
    }

    public Integer getPostCode() {
        return postCode;
    }
}
