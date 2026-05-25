package com.ctn.offerwall.user.wallet;

import com.ctn.offerwall.common.event.BusinessEvent;
import com.ctn.offerwall.common.event.EntityType;
import com.ctn.offerwall.common.event.EventMetadata;
import com.ctn.offerwall.common.event.EventOutcome;
import com.ctn.offerwall.common.event.EventType;
import com.ctn.offerwall.user.domain.AppUser;
import com.ctn.offerwall.user.domain.WalletCard;
import com.ctn.offerwall.user.exception.AuthenticationRequiredException;
import com.ctn.offerwall.user.exception.WalletCardNotFoundException;
import com.ctn.offerwall.user.repository.AppUserRepository;
import com.ctn.offerwall.user.repository.WalletCardRepository;
import com.ctn.offerwall.user.tracking.BusinessEventPublisher;
import com.ctn.offerwall.user.wallet.dto.WalletCardResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class WalletService {

    private final AppUserRepository userRepository;
    private final WalletCardRepository walletCardRepository;
    private final BusinessEventPublisher eventPublisher;

    public WalletService(AppUserRepository userRepository,
                         WalletCardRepository walletCardRepository,
                         BusinessEventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.walletCardRepository = walletCardRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(readOnly = true)
    public List<WalletCardResponse> listCards(UUID userId) {
        return walletCardRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(WalletCardResponse::from)
                .toList();
    }

    @Transactional
    public WalletCardResponse addCard(UUID userId, UUID cardProductId) {
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthenticationRequiredException("User no longer exists."));
        WalletCard walletCard = walletCardRepository.save(new WalletCard(user, cardProductId.toString()));
        publishWalletEvent(EventType.WALLET_CARD_ADDED, userId, walletCard);
        return WalletCardResponse.from(walletCard);
    }

    @Transactional
    public void deleteCard(UUID userId, UUID walletCardId) {
        WalletCard walletCard = walletCardRepository.findByIdAndUserId(walletCardId, userId)
                .orElseThrow(() -> new WalletCardNotFoundException("Wallet card was not found."));
        walletCardRepository.delete(walletCard);
        publishWalletEvent(EventType.WALLET_CARD_DELETED, userId, walletCard);
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
