package com.csci318.recommendationservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;

import java.util.UUID;

/**
 * Entity inside the PCBuild aggregate: the AI-generated explanation of why the build suits the customer.
 * Created only through {@link PCBuild#recommend(String)}.
 */
@Entity
public class Recommendation {

    @Id
    private String id;

    @Column(nullable = false)
    private String buildId;

    @Lob
    @Column(nullable = false)
    private String reasoning;

    protected Recommendation() {
    }

    Recommendation(String buildId, String reasoning) {
        this.id = UUID.randomUUID().toString();
        this.buildId = buildId;
        this.reasoning = reasoning;
    }

    public String getId() {
        return id;
    }

    public String getBuildId() {
        return buildId;
    }

    public String getReasoning() {
        return reasoning;
    }
}
