package com.ctn.offerwall.user.notification;

import com.ctn.offerwall.common.offer.OfferEligibilityMode;
import com.ctn.offerwall.user.domain.AppUser;
import com.ctn.offerwall.user.exception.UserInputException;
import com.ctn.offerwall.user.notification.dto.NotificationRecipientResponse;
import com.ctn.offerwall.user.notification.dto.OfferNotificationRecipientRequest;
import com.ctn.offerwall.user.repository.AppUserRepository;
import com.ctn.offerwall.user.repository.WalletCardRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificationRecipientService {

    private final AppUserRepository userRepository;
    private final WalletCardRepository walletCardRepository;

    public NotificationRecipientService(AppUserRepository userRepository,
                                        WalletCardRepository walletCardRepository) {
        this.userRepository = userRepository;
        this.walletCardRepository = walletCardRepository;
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
            case CRITERIA -> throw new UserInputException("CRITERIA recipient resolution is not implemented yet.");
        };

        return users.stream()
                .map(NotificationRecipientResponse::from)
                .toList();
    }
}
