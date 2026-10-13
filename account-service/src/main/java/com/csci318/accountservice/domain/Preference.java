package com.csci318.accountservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Lob;

/**
 * Value object: saved free-text preferences used for PC build recommendations (A3). Optional; a blank value
 * is treated as no preference. Stored as a CLOB, so there is no length limit.
 */
@Embeddable
public record Preference(@Lob @Column(name = "preference") String preference) {

    public Preference {
        if (preference != null && preference.isBlank()) {
            preference = null;
        }
    }
}
