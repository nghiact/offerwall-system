package com.ctn.offerwall.eligibility.eligibility;

import com.ctn.offerwall.common.card.CardNetwork;
import com.ctn.offerwall.common.card.CardType;
import com.ctn.offerwall.common.offer.OfferEligibilityMode;
import com.ctn.offerwall.eligibility.card.CardProductClient;
import com.ctn.offerwall.eligibility.card.CardProductSummary;
import com.ctn.offerwall.eligibility.eligibility.dto.BulkOfferEligibilityRequest;
import com.ctn.offerwall.eligibility.eligibility.dto.BulkOfferEligibilityResponse;
import com.ctn.offerwall.eligibility.eligibility.dto.OfferEligibilityCheckRequest;
import com.ctn.offerwall.eligibility.eligibility.dto.OfferEligibilityRequest;
import com.ctn.offerwall.eligibility.eligibility.dto.OfferEligibilityResponse;
import com.ctn.offerwall.eligibility.eligibility.dto.OfferEligibilityResult;
import com.ctn.offerwall.eligibility.eligibility.dto.UserWalletCandidate;
import com.ctn.offerwall.eligibility.exception.UserInputException;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class OfferEligibilityService {

    private final CardProductClient cardProductClient;

    public OfferEligibilityService(CardProductClient cardProductClient) {
        this.cardProductClient = cardProductClient;
    }

    public OfferEligibilityResponse resolveUsers(OfferEligibilityRequest request) {
        validateRequest(request);
        Map<UUID, CardProductSummary> criteriaCardsById = request.eligibilityMode() == OfferEligibilityMode.CRITERIA
                ? lookupCriteriaCards(request.candidates())
                : Map.of();
        return new OfferEligibilityResponse(resolveUsers(request, criteriaCardsById));
    }

    public BulkOfferEligibilityResponse resolveBulkUsers(BulkOfferEligibilityRequest request) {
        request.offers().forEach(this::validateRequest);
        Map<UUID, CardProductSummary> criteriaCardsById = request.offers().stream()
                .anyMatch(offer -> offer.eligibilityMode() == OfferEligibilityMode.CRITERIA)
                ? lookupCriteriaCards(request.candidates())
                : Map.of();

        List<OfferEligibilityResult> results = request.offers().stream()
                .map(offer -> {
                    OfferEligibilityRequest eligibilityRequest = toEligibilityRequest(offer, request.candidates());
                    return new OfferEligibilityResult(
                            offer.offerId(),
                            resolveUsers(eligibilityRequest, criteriaCardsById)
                    );
                })
                .toList();
        return new BulkOfferEligibilityResponse(results);
    }

    private List<UUID> resolveUsers(OfferEligibilityRequest request, Map<UUID, CardProductSummary> criteriaCardsById) {
        return switch (request.eligibilityMode()) {
            case ALL -> allCandidateUsers(request.candidates());
            case CARD_IDS -> cardIdCandidateUsers(request);
            case CRITERIA -> criteriaCandidateUsers(request, criteriaCardsById);
        };
    }

    private void validateRequest(OfferEligibilityRequest request) {
        switch (request.eligibilityMode()) {
            case ALL -> {
                if (hasTargetCardIds(request) || hasCriteria(request)) {
                    throw new UserInputException("ALL eligibility cannot define card IDs or criteria.");
                }
            }
            case CARD_IDS -> {
                if (!hasTargetCardIds(request)) {
                    throw new UserInputException("CARD_IDS eligibility requires at least one card product ID.");
                }
                if (hasCriteria(request)) {
                    throw new UserInputException("CARD_IDS eligibility cannot define criteria.");
                }
            }
            case CRITERIA -> {
                if (hasTargetCardIds(request)) {
                    throw new UserInputException("CRITERIA eligibility cannot define card product IDs.");
                }
                if (!hasCriteria(request)) {
                    throw new UserInputException("CRITERIA eligibility requires at least one criterion.");
                }
            }
        }
    }

    private boolean hasTargetCardIds(OfferEligibilityRequest request) {
        return request.targetCardProductIds().stream().anyMatch(Objects::nonNull);
    }

    private boolean hasCriteria(OfferEligibilityRequest request) {
        return request.targetIssuers().stream().anyMatch(value -> normalize(value) != null)
                || request.targetNetworks().stream().anyMatch(Objects::nonNull)
                || request.targetTier() != null
                || request.targetTypes().stream().anyMatch(Objects::nonNull)
                || request.targetPersonal() != null;
    }

    private void validateRequest(OfferEligibilityCheckRequest request) {
        validateRequest(toEligibilityRequest(request, List.of()));
    }

    private OfferEligibilityRequest toEligibilityRequest(OfferEligibilityCheckRequest request,
                                                         List<UserWalletCandidate> candidates) {
        return new OfferEligibilityRequest(
                request.eligibilityMode(),
                request.targetCardProductIds(),
                request.targetIssuers(),
                request.targetNetworks(),
                request.targetTier(),
                request.targetTypes(),
                request.targetPersonal(),
                candidates
        );
    }

    private List<UUID> allCandidateUsers(List<UserWalletCandidate> candidates) {
        return candidates.stream()
                .map(UserWalletCandidate::userId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private List<UUID> cardIdCandidateUsers(OfferEligibilityRequest request) {
        Set<UUID> targetIds = request.targetCardProductIds().stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (targetIds.isEmpty()) {
            return List.of();
        }

        return request.candidates().stream()
                .filter(candidate -> hasAnyCard(candidate, targetIds))
                .map(UserWalletCandidate::userId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private Map<UUID, CardProductSummary> lookupCriteriaCards(List<UserWalletCandidate> candidates) {
        List<UUID> candidateCardIds = candidates.stream()
                .flatMap(candidate -> candidate.cardProductIds().stream())
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (candidateCardIds.isEmpty()) {
            return Map.of();
        }

        return cardProductClient.lookupProducts(candidateCardIds).stream()
                .collect(Collectors.toMap(CardProductSummary::id, Function.identity()));
    }

    private List<UUID> criteriaCandidateUsers(OfferEligibilityRequest request,
                                              Map<UUID, CardProductSummary> cardsById) {
        if (cardsById.isEmpty()) {
            return List.of();
        }

        return request.candidates().stream()
                .filter(candidate -> hasMatchingCard(candidate, cardsById, request))
                .map(UserWalletCandidate::userId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private boolean hasAnyCard(UserWalletCandidate candidate, Set<UUID> targetIds) {
        return candidate.cardProductIds().stream().anyMatch(targetIds::contains);
    }

    private boolean hasMatchingCard(UserWalletCandidate candidate,
                                    Map<UUID, CardProductSummary> cardsById,
                                    OfferEligibilityRequest request) {
        return candidate.cardProductIds().stream()
                .map(cardsById::get)
                .filter(Objects::nonNull)
                .anyMatch(card -> matchesCriteria(card, request));
    }

    private boolean matchesCriteria(CardProductSummary card, OfferEligibilityRequest request) {
        return matchesIssuer(card.issuer(), request.targetIssuers())
                && matchesNetwork(card.network(), request.targetNetworks())
                && matchesTier(card.tier(), request.targetTier())
                && matchesType(card.type(), request.targetTypes())
                && matchesPersonal(card.personal(), request.targetPersonal());
    }

    private boolean matchesIssuer(String cardIssuer, List<String> targetIssuers) {
        Set<String> normalizedTargets = targetIssuers.stream()
                .map(this::normalize)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return normalizedTargets.isEmpty() || normalizedTargets.contains(normalize(cardIssuer));
    }

    private boolean matchesNetwork(CardNetwork cardNetwork, List<CardNetwork> targetNetworks) {
        return targetNetworks.isEmpty() || targetNetworks.contains(cardNetwork);
    }

    private boolean matchesTier(int cardTier, Integer targetTier) {
        return targetTier == null || cardTier >= targetTier;
    }

    private boolean matchesType(CardType cardType, List<CardType> targetTypes) {
        return targetTypes.isEmpty() || targetTypes.contains(cardType);
    }

    private boolean matchesPersonal(boolean cardPersonal, Boolean targetPersonal) {
        return targetPersonal == null || cardPersonal == targetPersonal;
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
