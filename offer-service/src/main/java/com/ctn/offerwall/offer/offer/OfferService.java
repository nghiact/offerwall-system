package com.ctn.offerwall.offer.offer;

import com.ctn.offerwall.common.event.BusinessEvent;
import com.ctn.offerwall.common.event.EntityType;
import com.ctn.offerwall.common.event.EventMetadata;
import com.ctn.offerwall.common.event.EventOutcome;
import com.ctn.offerwall.common.event.EventType;
import com.ctn.offerwall.common.offer.OfferEligibilityMode;
import com.ctn.offerwall.common.offer.OfferType;
import com.ctn.offerwall.offer.domain.Offer;
import com.ctn.offerwall.offer.domain.OfferCategory;
import com.ctn.offerwall.offer.domain.OfferStatus;
import com.ctn.offerwall.offer.exception.CategoryNotFoundException;
import com.ctn.offerwall.offer.exception.OfferNotFoundException;
import com.ctn.offerwall.offer.exception.UserInputException;
import com.ctn.offerwall.offer.offer.dto.OfferRequest;
import com.ctn.offerwall.offer.offer.dto.OfferResponse;
import com.ctn.offerwall.offer.repository.OfferCategoryRepository;
import com.ctn.offerwall.offer.repository.OfferRepository;
import com.ctn.offerwall.offer.tracking.BusinessEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class OfferService {

    private final OfferRepository offerRepository;
    private final OfferCategoryRepository categoryRepository;
    private final BusinessEventPublisher eventPublisher;
    private final Clock clock = Clock.systemUTC();

    public OfferService(OfferRepository offerRepository,
                        OfferCategoryRepository categoryRepository,
                        BusinessEventPublisher eventPublisher) {
        this.offerRepository = offerRepository;
        this.categoryRepository = categoryRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(readOnly = true)
    public List<OfferResponse> listOffers(UUID categoryId,
                                          OfferType offerType,
                                          OfferEligibilityMode eligibilityMode,
                                          OfferStatus status,
                                          String keyword) {
        Instant now = Instant.now(clock);
        String normalizedKeyword = trimToNull(keyword);
        String lowerKeyword = normalizedKeyword == null ? null : normalizedKeyword.toLowerCase(Locale.ROOT);

        return offerRepository.findAllWithDetails().stream()
                .filter(offer -> categoryId == null || offer.getCategory().getId().equals(categoryId))
                .filter(offer -> offerType == null || offer.getOfferType() == offerType)
                .filter(offer -> eligibilityMode == null || offer.getEligibilityMode() == eligibilityMode)
                .filter(offer -> status == null || offer.statusAt(now) == status)
                .filter(offer -> lowerKeyword == null || offer.getMerchantName().toLowerCase(Locale.ROOT).contains(lowerKeyword))
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
                trimToNull(request.targetIssuer()),
                request.targetNetwork(),
                request.targetTier(),
                request.targetType(),
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
                trimToNull(request.targetIssuer()),
                request.targetNetwork(),
                request.targetTier(),
                request.targetType(),
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
                    throw new UserInputException("CRITERIA eligibility requires at least one target criterion.");
                }
            }
        }
    }

    private boolean hasCriteria(OfferRequest request) {
        return trimToNull(request.targetIssuer()) != null
                || request.targetNetwork() != null
                || request.targetTier() != null
                || request.targetType() != null
                || request.targetPersonal() != null;
    }

    private Set<UUID> normalizeTargetCardProductIds(List<UUID> targetCardProductIds) {
        if (targetCardProductIds == null || targetCardProductIds.isEmpty()) {
            return Set.of();
        }
        return new LinkedHashSet<>(targetCardProductIds);
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
