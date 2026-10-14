package com.csci318.recommendationservice.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderColumn;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Aggregate root: the PC build the AI generated for a BuildRequest (referenced by requestId).
 * Owns its BuildComponents and its Recommendation (PCBuild 1 — 0..1 Recommendation).
 * The total price is always the sum of the component prices.
 */
@Entity
public class PCBuild {

    @Id
    private String id;

    @Column(nullable = false)
    private String requestId;

    @Column(nullable = false)
    private Float totalPrice;

    @Lob
    @Column(nullable = false)
    private String description;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "build_id", nullable = false)
    @OrderColumn(name = "position")
    private List<BuildComponent> components = new ArrayList<>();

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "recommendation_id")
    private Recommendation recommendation;

    protected PCBuild() {
    }

    public PCBuild(String requestId, String description, List<BuildComponent> components) {
        if (requestId == null || requestId.isBlank()) {
            throw new IllegalArgumentException("A PC build needs a requestId");
        }
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("A PC build needs a description");
        }
        if (components == null || components.isEmpty()) {
            throw new IllegalArgumentException("A PC build needs at least one component");
        }
        this.id = UUID.randomUUID().toString();
        this.requestId = requestId;
        this.description = description.trim();
        this.components.addAll(components);
        this.totalPrice = (float) components.stream().mapToDouble(BuildComponent::getPrice).sum();
    }

    /** Attaches the AI-generated recommendation to this build; a build has at most one recommendation. */
    public Recommendation recommend(String reasoning) {
        if (recommendation != null) {
            throw new IllegalStateException("The build already has a recommendation");
        }
        if (reasoning == null || reasoning.isBlank()) {
            throw new IllegalArgumentException("A recommendation needs a reasoning");
        }
        recommendation = new Recommendation(id, reasoning.trim());
        return recommendation;
    }

    public String getId() {
        return id;
    }

    public String getRequestId() {
        return requestId;
    }

    public Float getTotalPrice() {
        return totalPrice;
    }

    public String getDescription() {
        return description;
    }

    public List<BuildComponent> getComponents() {
        return List.copyOf(components);
    }

    public Recommendation getRecommendation() {
        return recommendation;
    }
}
