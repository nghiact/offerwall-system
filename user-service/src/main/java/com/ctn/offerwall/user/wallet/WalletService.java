package com.ctn.offerwall.user.wallet;

import com.ctn.offerwall.common.event.BusinessEvent;
import com.ctn.offerwall.common.event.EntityType;
import com.ctn.offerwall.common.event.EventMetadata;
import com.ctn.offerwall.common.event.EventOutcome;
import com.ctn.offerwall.common.event.EventType;
import com.ctn.offerwall.user.card.CardProductClient;
import com.ctn.offerwall.user.card.dto.CardProductSummaryResponse;
import com.ctn.offerwall.user.domain.AppUser;
import com.ctn.offerwall.user.domain.WalletCard;
import com.ctn.offerwall.user.eligibility.dto.UserWalletCandidate;
import com.ctn.offerwall.user.exception.AuthenticationRequiredException;
import com.ctn.offerwall.user.exception.UserInputException;
import com.ctn.offerwall.user.exception.WalletCardNotFoundException;
import com.ctn.offerwall.user.repository.AppUserRepository;
import com.ctn.offerwall.user.repository.WalletCardRepository;
import com.ctn.offerwall.user.tracking.BusinessEventPublisher;
import com.ctn.offerwall.user.wallet.dto.WalletCardResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class WalletService {

    private final AppUserRepository userRepository;
    private final WalletCardRepository walletCardRepository;
    private final BusinessEventPublisher eventPublisher;
    private final CardProductClient cardProductClient;

    public WalletService(AppUserRepository userRepository,
                         WalletCardRepository walletCardRepository,
                         BusinessEventPublisher eventPublisher,
                         CardProductClient cardProductClient) {
        this.userRepository = userRepository;
        this.walletCardRepository = walletCardRepository;
        this.eventPublisher = eventPublisher;
        this.cardProductClient = cardProductClient;
    }

    @Transactional(readOnly = true)
    public List<WalletCardResponse> listCards(UUID userId) {
        List<WalletCard> walletCards = walletCardRepository.findByUserIdOrderByCreatedAtDesc(userId);
        Map<UUID, CardProductSummaryResponse> productsById = productsById(walletCards);

        return walletCards.stream()
                .map(walletCard -> WalletCardResponse.from(
                        walletCard,
                        parseCardProductId(walletCard.getCardId())
                                .map(productsById::get)
                                .orElse(null)
                ))
                .toList();
    }

    @Transactional
    public WalletCardResponse addCard(UUID userId, UUID cardProductId) {
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthenticationRequiredException("User no longer exists."));
        CardProductSummaryResponse cardProduct = requireCardProduct(cardProductId);
        WalletCard walletCard = walletCardRepository.save(new WalletCard(user, cardProductId.toString()));
        publishWalletEvent(EventType.WALLET_CARD_ADDED, userId, walletCard);
        return WalletCardResponse.from(walletCard, cardProduct);
    }

    @Transactional
    public void deleteCard(UUID userId, UUID walletCardId) {
        WalletCard walletCard = walletCardRepository.findByIdAndUserId(walletCardId, userId)
                .orElseThrow(() -> new WalletCardNotFoundException("Wallet card was not found."));
        walletCardRepository.delete(walletCard);
        publishWalletEvent(EventType.WALLET_CARD_DELETED, userId, walletCard);
    }

    @Transactional(readOnly = true)
    public UserWalletCandidate walletCandidate(UUID userId) {
        List<UUID> cardProductIds = walletCardRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(WalletCard::getCardId)
                .map(this::parseCardProductId)
                .flatMap(Optional::stream)
                .distinct()
                .toList();
        return new UserWalletCandidate(userId, cardProductIds);
    }

    private CardProductSummaryResponse requireCardProduct(UUID cardProductId) {
        return cardProductClient.lookupProducts(List.of(cardProductId)).stream()
                .filter(product -> cardProductId.equals(product.id()))
                .findFirst()
                .orElseThrow(() -> new UserInputException("Card product was not found."));
    }

    private Map<UUID, CardProductSummaryResponse> productsById(List<WalletCard> walletCards) {
        List<UUID> cardProductIds = walletCards.stream()
                .map(WalletCard::getCardId)
                .map(this::parseCardProductId)
                .flatMap(Optional::stream)
                .distinct()
                .toList();
        if (cardProductIds.isEmpty()) {
            return Map.of();
        }

        return cardProductClient.lookupProducts(cardProductIds).stream()
                .collect(Collectors.toMap(CardProductSummaryResponse::id, Function.identity()));
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

    private void publishWalletEvent(EventType eventType, UUID userId, WalletCard walletCard) {
        eventPublisher.publish(new BusinessEvent(
                null,
                eventType,
                EventOutcome.SUCCESS,
                EntityType.WALLET_CARD,
                walletCard.getId().toString(),
                userId.toString(),
                null,
                new EventMetadata(Map.of("cardProductId", walletCard.getCardId()))
        ));
    }
}
