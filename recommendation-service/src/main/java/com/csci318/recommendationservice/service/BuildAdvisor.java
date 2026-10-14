package com.csci318.recommendationservice.service;

import java.util.List;

/**
 * Port to the LLM that proposes a PC build (R1). The answer is checked by Recommendation Service afterwards.
 */
public interface BuildAdvisor {

    /**
     * Asks the LLM for a build from the given context.
     *
     * @throws ServiceUnavailableException when the LLM is not configured, fails or times out
     */
    BuildProposal proposeBuild(BuildContext context);

    /**
     * Asks the LLM once more, telling it why its previous answer was rejected.
     *
     * @throws ServiceUnavailableException when the LLM is not configured, fails or times out
     */
    BuildProposal reviseBuild(BuildContext context, BuildProposal rejected, List<String> problems);
}
