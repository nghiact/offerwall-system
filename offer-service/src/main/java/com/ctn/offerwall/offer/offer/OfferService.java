package com.ctn.offerwall.offer.offer;

import com.ctn.offerwall.common.event.BusinessEvent;
import com.ctn.offerwall.common.event.EntityType;
import com.ctn.offerwall.common.event.EventMetadata;
import com.ctn.offerwall.common.event.EventOutcome;
import com.ctn.offerwall.common.event.EventType;
import com.ctn.offerwall.common.card.CardNetwork;
import com.ctn.offerwall.common.card.CardType;
import com.ctn.offerwall.common.offer.OfferEligibilityMode;
import com.ctn.offerwall.common.offer.OfferType;
import com.ctn.offerwall.offer.domain.Offer;
import com.ctn.offerwall.offer.domain.OfferCategory;
import com.ctn.offerwall.offer.domain.OfferStatus;
import com.ctn.offerwall.offer.eligibility.EligibilityClient;
import com.ctn.offerwall.offer.eligibility.dto.BulkOfferEligibilityRequest;
import com.ctn.offerwall.offer.eligibility.dto.OfferEligibilityCheckRequest;
import com.ctn.offerwall.offer.exception.CategoryNotFoundException;
import com.ctn.offerwall.offer.exception.OfferNotFoundException;
import com.ctn.offerwall.offer.exception.UserInputException;
import com.ctn.offerwall.offer.offer.dto.OfferRequest;
import com.ctn.offerwall.offer.offer.dto.OfferResponse;
import com.ctn.offerwall.offer.repository.OfferCategoryRepository;
import com.ctn.offerwall.offer.repository.OfferRepository;
import com.ctn.offerwall.offer.tracking.BusinessEventPublisher;
import com.ctn.offerwall.offer.user.UserWalletClient;
import com.ctn.offerwall.offer.user.dto.UserWalletCandidate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class OfferService {

    private final OfferRepository offerRepository;
    private final OfferCategoryRepository categoryRepository;
    private final BusinessEventPublisher eventPublisher;
    private final UserWalletClient userWalletClient;
    private final EligibilityClient eligibilityClient;
    private final Clock clock = Clock.systemUTC();

    public OfferService(OfferRepository offerRepository,
                        OfferCategoryRepository categoryRepository,
                        BusinessEventPublisher eventPublisher,
                        UserWalletClient userWalletClient,
                        EligibilityClient eligibilityClient) {
        this.offerRepository = offerRepository;
        this.categoryRepository = categoryRepository;
        this.eventPublisher = eventPublisher;
        this.userWalletClient = userWalletClient;
        this.eligibilityClient = eligibilityClient;
    }

    @Transactional(readOnly = true)
    public List<OfferResponse> listOffers(UUID categoryId,
                                          OfferType offerType,
                                          OfferEligibilityMode eligibilityMode,
                                          OfferStatus status,
                                          String keyword) {
        Instant now = Instant.now(clock);
        return filteredOffers(categoryId, offerType, eligibilityMode, status, keyword, now).stream()
                .map(offer -> toResponse(offer, now))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OfferResponse> listOffersAvailableForUser(UUID userId,
                                                          UUID categoryId,
                                                          OfferType offerType,
                                                          OfferEligibilityMode eligibilityMode,
                                                          OfferStatus status,
                                                          String keyword) {
        Instant now = Instant.now(clock);
        UserWalletCandidate walletCandidate = userWalletClient.getWalletCandidate(userId);
        List<com.ctn.offerwall.offer.eligibility.dto.UserWalletCandidate> candidates = List.of(
                new com.ctn.offerwall.offer.eligibility.dto.UserWalletCandidate(userId, walletCandidate.cardProductIds())
        );
        List<Offer> offers = filteredOffers(categoryId, offerType, eligibilityMode, status, keyword, now);
        Set<UUID> eligibleOfferIds = eligibilityClient.resolveEligibleOffers(toBulkEligibilityRequest(offers, candidates)).stream()
                .filter(result -> result.eligibleUserIds().contains(userId))
                .map(com.ctn.offerwall.offer.eligibility.dto.OfferEligibilityResult::offerId)
                .collect(Collectors.toSet());

        return offers.stream()
                .filter(offer -> eligibleOfferIds.contains(offer.getId()))
                .map(offer -> toResponse(offer, now))
                .toList();
    }

    @Transactional(readOnly = true)
    public OfferResponse getOffer(UUID id) {
        return toResponse(findOffer(id), Instant.now(clock));
    }

    @Transactional
    public OfferResponse createOffer(OfferRequest request, String actorUserId) {
        OfferCategory category = findCategory(request.categoryId());
        validateRequest(request);
        Set<UUID> targetCardProductIds = normalizeTargetCardProductIds(request.targetCardProductIds());
        Set<String> targetIssuers = normalizeTargetIssuers(request);
        Set<CardNetwork> targetNetworks = normalizeTargetNetworks(request);
        Set<CardType> targetTypes = normalizeTargetTypes(request);

        Offer offer = new Offer(
                category,
                request.merchantName().trim(),
                request.offerSummary().trim(),
                request.addressDisplay().trim(),
                trimToNull(request.addressUrl()),
                request.startTime(),
                request.endTime(),
                request.offerType(),
                request.eligibilityMode(),
                targetCardProductIds,
                targetIssuers,
                targetNetworks,
                request.targetTier(),
                targetTypes,
                request.targetPersonal()
        );
        Offer savedOffer = offerRepository.save(offer);
        publishOfferCreated(savedOffer, actorUserId);
        return toResponse(savedOffer, Instant.now(clock));
    }

    @Transactional
    public OfferResponse updateOffer(UUID id, OfferRequest request) {
        Offer offer = findOffer(id);
        OfferCategory category = findCategory(request.categoryId());
        validateRequest(request);
        Set<UUID> targetCardProductIds = normalizeTargetCardProductIds(request.targetCardProductIds());
        Set<String> targetIssuers = normalizeTargetIssuers(request);
        Set<CardNetwork> targetNetworks = normalizeTargetNetworks(request);
        Set<CardType> targetTypes = normalizeTargetTypes(request);

        offer.update(
                category,
                request.merchantName().trim(),
                request.offerSummary().trim(),
                request.addressDisplay().trim(),
                trimToNull(request.addressUrl()),
                request.startTime(),
                request.endTime(),
                request.offerType(),
                request.eligibilityMode(),
                targetCardProductIds,
                targetIssuers,
                targetNetworks,
                request.targetTier(),
                targetTypes,
                request.targetPersonal()
        );
        return toResponse(offer, Instant.now(clock));
    }

    @Transactional
    public void deleteOffer(UUID id) {
        Offer offer = findOffer(id);
        offerRepository.delete(offer);
    }

    private Offer findOffer(UUID id) {
        return offerRepository.findById(id)
                .orElseThrow(() -> new OfferNotFoundException("Offer was not found."));
    }

    private OfferCategory findCategory(UUID id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException("Offer category was not found."));
    }

    private OfferResponse toResponse(Offer offer, Instant now) {
        return OfferResponse.from(offer, offer.statusAt(now));
    }

    private void publishOfferCreated(Offer offer, String actorUserId) {
        eventPublisher.publish(new BusinessEvent(
                null,
                EventType.OFFER_CREATED,
                EventOutcome.SUCCESS,
                EntityType.OFFER,
                offer.getId().toString(),
                actorUserId,
                null,
                new EventMetadata(Map.of(
                        "categoryId", offer.getCategory().getId().toString(),
                        "merchantName", offer.getMerchantName(),
                        "offerType", offer.getOfferType().name(),
                        "eligibilityMode", offer.getEligibilityMode().name()
                ))
        ));
    }

    private void validateRequest(OfferRequest request) {
        if (!request.endTime().isAfter(request.startTime())) {
            throw new UserInputException("Offer endTime must be after startTime.");
        }

        Set<UUID> targetCardProductIds = normalizeTargetCardProductIds(request.targetCardProductIds());
        boolean hasCriteria = hasCriteria(request);

        switch (request.eligibilityMode()) {
            case ALL -> {
                if (!targetCardProductIds.isEmpty() || hasCriteria) {
                    throw new UserInputException("ALL eligibility cannot define card IDs or criteria.");
                }
            }
            case CARD_IDS -> {
                if (targetCardProductIds.isEmpty()) {
                    throw new UserInputException("CARD_IDS eligibility requires at least one card product ID.");
                }
                if (hasCriteria) {
                    throw new UserInputException("CARD_IDS eligibility cannot define criteria.");
                }
            }
            case CRITERIA -> {
                if (!targetCardProductIds.isEmpty()) {
                    throw new UserInputException("CRITERIA eligibility cannot define card product IDs.");
                }
                if (!hasCriteria) {
                    throw new UserInputException("CRITERIA eligibility requires at least one criterion.");
                }
            }
        }
    }

    private List<Offer> filteredOffers(UUID categoryId,
                                       OfferType offerType,
                                       OfferEligibilityMode eligibilityMode,
                                       OfferStatus status,
                                       String keyword,
                                       Instant now) {
        String normalizedKeyword = trimToNull(keyword);
        String lowerKeyword = normalizedKeyword == null ? null : normalizedKeyword.toLowerCase(Locale.ROOT);

        return offerRepository.findAllWithDetails().stream()
                .filter(offer -> categoryId == null || offer.getCategory().getId().equals(categoryId))
                .filter(offer -> offerType == null || offer.getOfferType() == offerType)
                .filter(offer -> eligibilityMode == null || offer.getEligibilityMode() == eligibilityMode)
                .filter(offer -> status == null || offer.statusAt(now) == status)
                .filter(offer -> lowerKeyword == null || offer.getMerchantName().toLowerCase(Locale.ROOT).contains(lowerKeyword))
                .toList();
    }

    private BulkOfferEligibilityRequest toBulkEligibilityRequest(
            List<Offer> offers,
            List<com.ctn.offerwall.offer.eligibility.dto.UserWalletCandidate> candidates) {
        return new BulkOfferEligibilityRequest(
                offers.stream()
                        .map(this::toEligibilityCheck)
                        .toList(),
                candidates
        );
    }

    private OfferEligibilityCheckRequest toEligibilityCheck(Offer offer) {
        return new OfferEligibilityCheckRequest(
                offer.getId(),
                offer.getEligibilityMode(),
                offer.getTargetCardProductIds().stream().toList(),
                offer.getTargetIssuers().stream().toList(),
                offer.getTargetNetworks().stream().toList(),
                offer.getTargetTier(),
                offer.getTargetTypes().stream().toList(),
                offer.getTargetPersonal()
        );
    }

    private boolean hasCriteria(OfferRequest request) {
        return !normalizeTargetIssuers(request).isEmpty()
                || !normalizeTargetNetworks(request).isEmpty()
                || request.targetTier() != null
                || !normalizeTargetTypes(request).isEmpty()
                || request.targetPersonal() != null;
    }

    private Set<UUID> normalizeTargetCardProductIds(List<UUID> targetCardProductIds) {
        if (targetCardProductIds == null || targetCardProductIds.isEmpty()) {
            return Set.of();
        }
        return new LinkedHashSet<>(targetCardProductIds);
    }

    private Set<String> normalizeTargetIssuers(OfferRequest request) {
        LinkedHashSet<String> values = new LinkedHashSet<>();
        addIssuer(values, request.targetIssuer());
        if (request.targetIssuers() != null) {
            request.targetIssuers().forEach(value -> addIssuer(values, value));
        }
        return values;
    }

    private Set<CardNetwork> normalizeTargetNetworks(OfferRequest request) {
        LinkedHashSet<CardNetwork> values = new LinkedHashSet<>();
        if (request.targetNetwork() != null) {
            values.add(request.targetNetwork());
        }
        if (request.targetNetworks() != null) {
            request.targetNetworks().stream()
                    .filter(Objects::nonNull)
                    .forEach(values::add);
        }
        return values;
    }

    private Set<CardType> normalizeTargetTypes(OfferRequest request) {
        LinkedHashSet<CardType> values = new LinkedHashSet<>();
        if (request.targetType() != null) {
            values.add(request.targetType());
        }
        if (request.targetTypes() != null) {
            request.targetTypes().stream()
                    .filter(Objects::nonNull)
                    .forEach(values::add);
        }
        return values;
    }

    private void addIssuer(Set<String> values, String issuer) {
        String normalized = trimToNull(issuer);
        if (normalized != null) {
            values.add(normalized);
        }
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
