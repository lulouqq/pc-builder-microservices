package com.csci318.recommendationservice.service;

import com.csci318.recommendationservice.domain.BuildComponent;
import com.csci318.recommendationservice.domain.BuildRequest;
import com.csci318.recommendationservice.domain.ComponentType;
import com.csci318.recommendationservice.domain.PCBuild;
import com.csci318.recommendationservice.domain.ValidationException;
import com.csci318.recommendationservice.domain.event.BuildRequested;
import com.csci318.recommendationservice.domain.event.RecommendationGenerated;
import com.csci318.recommendationservice.infrastructure.BuildRequestRepository;
import com.csci318.recommendationservice.infrastructure.PCBuildRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Application service for R1 (generate a recommended build), R3 (view one recommendation) and
 * R4 (a customer's previous recommendations).
 */
@Service
public class RecommendationService {

    private final BuildRequestRepository buildRequestRepository;
    private final PCBuildRepository pcBuildRepository;
    private final AccountClient accountClient;
    private final CatalogClient catalogClient;
    private final InventoryClient inventoryClient;
    private final BuildAdvisor buildAdvisor;
    private final ApplicationEventPublisher eventPublisher;
    private final TransactionTemplate transactionTemplate;
    private final BuildProposalValidator validator = new BuildProposalValidator();

    public RecommendationService(BuildRequestRepository buildRequestRepository, PCBuildRepository pcBuildRepository,
                                 AccountClient accountClient, CatalogClient catalogClient,
                                 InventoryClient inventoryClient, BuildAdvisor buildAdvisor,
                                 ApplicationEventPublisher eventPublisher,
                                 PlatformTransactionManager transactionManager) {
        this.buildRequestRepository = buildRequestRepository;
        this.pcBuildRepository = pcBuildRepository;
        this.accountClient = accountClient;
        this.catalogClient = catalogClient;
        this.inventoryClient = inventoryClient;
        this.buildAdvisor = buildAdvisor;
        this.eventPublisher = eventPublisher;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    /**
     * R1. Collects the saved preference (Account), the products (Catalog) and their stock (Inventory), asks the
     * LLM for a build, re-checks the answer and retries once with the problems found. The request, build and
     * recommendation are stored only when a valid build was generated.
     *
     * @param currentUserId       the authenticated user (token subject); must be the request's userId
     * @param authorizationHeader forwarded to Account Service
     */
    public RecommendationView generate(GenerateRecommendationCommand command, String currentUserId,
                                       String authorizationHeader) {
        if (command.userId() == null || command.userId().isBlank()) {
            throw new ValidationException("userId is required");
        }
        if (!command.userId().equals(currentUserId)) {
            throw new AccessDeniedException("A recommendation can only be requested for your own account");
        }
        BuildRequest buildRequest = new BuildRequest(command.userId(), command.budget(), command.purpose(),
                command.preferences());

        String savedPreference = accountClient.getSavedPreference(buildRequest.getUserId(), authorizationHeader)
                .orElse(null);
        List<CatalogProduct> availableProducts = availableProducts();
        requireBuildIsPossible(availableProducts, buildRequest.getBudget());

        BuildContext context = new BuildContext(buildRequest.getBudget(), buildRequest.getPurpose(),
                buildRequest.getPreferences(), savedPreference, availableProducts);
        Map<String, CatalogProduct> productsById = availableProducts.stream()
                .collect(Collectors.toMap(CatalogProduct::id, Function.identity()));

        BuildProposal proposal = buildAdvisor.proposeBuild(context);
        List<String> problems = validator.validate(proposal, productsById, buildRequest.getBudget());
        if (!problems.isEmpty()) {
            proposal = buildAdvisor.reviseBuild(context, proposal, problems);
            problems = validator.validate(proposal, productsById, buildRequest.getBudget());
            if (!problems.isEmpty()) {
                throw new NoValidBuildException(
                        "No valid PC build could be generated: " + String.join(" ", problems));
            }
        }

        List<BuildComponent> components = proposal.productIds().stream()
                .map(productsById::get)
                .map(product -> new BuildComponent(product.id(), product.componentType().orElseThrow(),
                        product.price()))
                .toList();
        PCBuild build = new PCBuild(buildRequest.getId(), proposal.description(), components);
        build.recommend(proposal.reasoning());

        transactionTemplate.executeWithoutResult(status -> {
            buildRequestRepository.save(buildRequest);
            pcBuildRepository.save(build);
        });
        eventPublisher.publishEvent(BuildRequested.of(buildRequest));
        eventPublisher.publishEvent(RecommendationGenerated.of(buildRequest, build));
        return new RecommendationView(buildRequest, build);
    }

    /** R3: one recommendation; only its owner may view it (unknown id 404, another user's 403). */
    @Transactional(readOnly = true)
    public RecommendationView getRecommendation(String recommendationId, String currentUserId) {
        PCBuild build = pcBuildRepository.findByRecommendationId(recommendationId)
                .orElseThrow(() -> new ResourceNotFoundException("Recommendation not found: " + recommendationId));
        BuildRequest request = buildRequestRepository.findById(build.getRequestId())
                .orElseThrow(() -> new IllegalStateException("Build request missing for build " + build.getId()));
        if (!request.getUserId().equals(currentUserId)) {
            throw new AccessDeniedException("You do not have permission to access this recommendation");
        }
        return new RecommendationView(request, build);
    }

    /** R4: the customer's previous build requests with their recommendations, in no guaranteed order. */
    @Transactional(readOnly = true)
    public List<RecommendationView> getUserRecommendations(String userId) {
        List<BuildRequest> requests = buildRequestRepository.findByUserId(userId);
        Map<String, PCBuild> buildsByRequestId = pcBuildRepository
                .findByRequestIdIn(requests.stream().map(BuildRequest::getId).toList()).stream()
                .collect(Collectors.toMap(PCBuild::getRequestId, Function.identity()));
        return requests.stream()
                .filter(request -> buildsByRequestId.containsKey(request.getId()))
                .map(request -> new RecommendationView(request, buildsByRequestId.get(request.getId())))
                .toList();
    }

    /** Catalog products in a canonical category that Inventory reports as in stock. */
    private List<CatalogProduct> availableProducts() {
        return catalogClient.getProducts().stream()
                .filter(product -> product.componentType().isPresent())
                .filter(product -> inventoryClient.isInStock(product.id()))
                .toList();
    }

    /** Fails fast (409) without asking the LLM when no build can satisfy the rules. */
    private void requireBuildIsPossible(List<CatalogProduct> availableProducts, Float budget) {
        Map<ComponentType, Float> cheapestByType = new EnumMap<>(ComponentType.class);
        for (CatalogProduct product : availableProducts) {
            cheapestByType.merge(product.componentType().orElseThrow(), product.price(), Math::min);
        }
        List<String> missing = Arrays.stream(ComponentType.values())
                .filter(type -> !cheapestByType.containsKey(type))
                .map(ComponentType::categoryName)
                .toList();
        if (!missing.isEmpty()) {
            throw new NoValidBuildException("No product is in stock for: " + String.join(", ", missing));
        }
        float cheapestTotal = (float) cheapestByType.values().stream().mapToDouble(Float::doubleValue).sum();
        if (cheapestTotal > budget) {
            throw new NoValidBuildException("The cheapest possible build costs "
                    + BuildProposalValidator.price(cheapestTotal) + ", which is over the budget "
                    + BuildProposalValidator.price(budget));
        }
    }
}
