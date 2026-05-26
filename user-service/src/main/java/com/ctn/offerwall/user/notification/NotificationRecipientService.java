package com.ctn.offerwall.user.notification;

import com.ctn.offerwall.common.offer.OfferEligibilityMode;
import com.ctn.offerwall.common.card.CardNetwork;
import com.ctn.offerwall.common.card.CardType;
import com.ctn.offerwall.user.domain.AppUser;
import com.ctn.offerwall.user.domain.WalletCard;
import com.ctn.offerwall.user.eligibility.EligibilityClient;
import com.ctn.offerwall.user.eligibility.dto.OfferEligibilityRequest;
import com.ctn.offerwall.user.eligibility.dto.UserWalletCandidate;
import com.ctn.offerwall.user.notification.dto.NotificationRecipientResponse;
import com.ctn.offerwall.user.notification.dto.OfferNotificationRecipientRequest;
import com.ctn.offerwall.user.repository.AppUserRepository;
import com.ctn.offerwall.user.repository.WalletCardRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class NotificationRecipientService {

    private final AppUserRepository userRepository;
    private final WalletCardRepository walletCardRepository;
    private final EligibilityClient eligibilityClient;

    public NotificationRecipientService(AppUserRepository userRepository,
                                        WalletCardRepository walletCardRepository,
                                        EligibilityClient eligibilityClient) {
        this.userRepository = userRepository;
        this.walletCardRepository = walletCardRepository;
        this.eligibilityClient = eligibilityClient;
    }

    @Transactional(readOnly = true)
    public List<NotificationRecipientResponse> resolveOfferRecipients(OfferNotificationRecipientRequest request) {
        List<AppUser> users = switch (request.eligibilityMode()) {
            case ALL -> userRepository.findAll();
            case CARD_IDS -> request.targetCardProductIds().isEmpty()
                    ? List.of()
                    : walletCardRepository.findDistinctUsersByCardIdIn(
                            request.targetCardProductIds().stream()
                                    .map(Object::toString)
                                    .toList()
                    );
            case CRITERIA -> resolveCriteriaRecipients(request);
        };

        return users.stream()
                .map(NotificationRecipientResponse::from)
                .toList();
    }

    private List<AppUser> resolveCriteriaRecipients(OfferNotificationRecipientRequest request) {
        List<WalletCandidate> candidates = walletCandidates();
        if (candidates.isEmpty()) {
            return List.of();
        }

        Set<UUID> eligibleUserIds = new LinkedHashSet<>(eligibilityClient.resolveEligibleUsers(toEligibilityRequest(request, candidates)));
        return candidates.stream()
                .filter(candidate -> eligibleUserIds.contains(candidate.user().getId()))
                .map(WalletCandidate::user)
                .toList();
    }

    private List<WalletCandidate> walletCandidates() {
        Map<UUID, WalletCandidateAccumulator> candidatesByUser = new LinkedHashMap<>();
        for (WalletCard walletCard : walletCardRepository.findAllWithUsers()) {
            Optional<UUID> cardProductId = parseCardProductId(walletCard.getCardId());
            if (cardProductId.isEmpty()) {
                continue;
            }

            WalletCandidateAccumulator candidate = candidatesByUser.computeIfAbsent(
                    walletCard.getUser().getId(),
                    ignored -> new WalletCandidateAccumulator(walletCard.getUser())
            );
            candidate.cardProductIds().add(cardProductId.get());
        }

        return candidatesByUser.values().stream()
                .map(WalletCandidateAccumulator::toCandidate)
                .toList();
    }

    private OfferEligibilityRequest toEligibilityRequest(OfferNotificationRecipientRequest request,
                                                         List<WalletCandidate> candidates) {
        return new OfferEligibilityRequest(
                request.eligibilityMode(),
                request.targetCardProductIds(),
                targetIssuers(request),
                targetNetworks(request),
                request.targetTier(),
                targetTypes(request),
                request.targetPersonal(),
                candidates.stream()
                        .map(candidate -> new UserWalletCandidate(candidate.user().getId(), candidate.cardProductIds()))
                        .toList()
        );
    }

    private List<String> targetIssuers(OfferNotificationRecipientRequest request) {
        LinkedHashSet<String> values = new LinkedHashSet<>(request.targetIssuers());
        if (request.targetIssuer() != null && !request.targetIssuer().isBlank()) {
            values.add(request.targetIssuer().trim());
        }
        return List.copyOf(values);
    }

    private List<CardNetwork> targetNetworks(OfferNotificationRecipientRequest request) {
        LinkedHashSet<CardNetwork> values = new LinkedHashSet<>(request.targetNetworks());
        if (request.targetNetwork() != null) {
            values.add(request.targetNetwork());
        }
        return List.copyOf(values);
    }

    private List<CardType> targetTypes(OfferNotificationRecipientRequest request) {
        LinkedHashSet<CardType> values = new LinkedHashSet<>(request.targetTypes());
        if (request.targetType() != null) {
            values.add(request.targetType());
        }
        return List.copyOf(values);
    }

    private Optional<UUID> parseCardProductId(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(value));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    private record WalletCandidate(AppUser user, List<UUID> cardProductIds) {
    }

    private record WalletCandidateAccumulator(AppUser user, Set<UUID> cardProductIds) {

        private WalletCandidateAccumulator(AppUser user) {
            this(user, new LinkedHashSet<>());
        }

        private WalletCandidate toCandidate() {
            return new WalletCandidate(user, List.copyOf(cardProductIds));
        }
    }
}
