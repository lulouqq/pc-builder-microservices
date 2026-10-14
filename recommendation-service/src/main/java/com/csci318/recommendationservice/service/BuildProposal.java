package com.csci318.recommendationservice.service;

import java.util.List;

/**
 * The LLM's answer for R1. {@code readable} is false when the answer was not the expected JSON; the raw answer
 * is kept so that it can be sent back to the LLM with the validation feedback.
 */
public record BuildProposal(String rawAnswer, boolean readable, List<String> productIds, String description,
                            String reasoning) {

    public BuildProposal {
        productIds = productIds == null ? List.of() : productIds.stream().filter(id -> id != null).toList();
    }

    public static BuildProposal of(String rawAnswer, List<String> productIds, String description, String reasoning) {
        return new BuildProposal(rawAnswer, true, productIds, description, reasoning);
    }

    public static BuildProposal unreadable(String rawAnswer) {
        return new BuildProposal(rawAnswer, false, List.of(), null, null);
    }
}
