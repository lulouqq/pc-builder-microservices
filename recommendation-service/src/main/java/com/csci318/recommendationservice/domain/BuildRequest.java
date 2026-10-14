package com.csci318.recommendationservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;

import java.util.UUID;

/**
 * Aggregate root: what the customer asked for in R1 (budget, intended use and the preference from the request).
 * The preference saved in Account Service is not stored here; it is only extra context for the LLM.
 */
@Entity
public class BuildRequest {

    @Id
    private String id;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private Float budget;

    @Lob
    @Column(nullable = false)
    private String purpose;

    @Lob
    private String preferences;

    protected BuildRequest() {
    }

    public BuildRequest(String userId, Float budget, String purpose, String preferences) {
        if (userId == null || userId.isBlank()) {
            throw new ValidationException("userId is required");
        }
        if (budget == null || !(budget > 0) || budget.isInfinite()) {
            throw new ValidationException("budget must be greater than 0");
        }
        if (purpose == null || purpose.isBlank()) {
            throw new ValidationException("purpose is required");
        }
        this.id = UUID.randomUUID().toString();
        this.userId = userId;
        this.budget = budget;
        this.purpose = purpose.trim();
        this.preferences = preferences == null || preferences.isBlank() ? null : preferences.trim();
    }

    public String getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public Float getBudget() {
        return budget;
    }

    public String getPurpose() {
        return purpose;
    }

    public String getPreferences() {
        return preferences;
    }
}
