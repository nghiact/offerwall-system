package com.ctn.offerwall.user.user;

import com.ctn.offerwall.common.event.BusinessEvent;
import com.ctn.offerwall.common.event.EntityType;
import com.ctn.offerwall.common.event.EventMetadata;
import com.ctn.offerwall.common.event.EventOutcome;
import com.ctn.offerwall.common.event.EventType;
import com.ctn.offerwall.user.domain.AppUser;
import com.ctn.offerwall.user.exception.AuthenticationRequiredException;
import com.ctn.offerwall.user.repository.AppUserRepository;
import com.ctn.offerwall.user.tracking.BusinessEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class UserService {

    private final AppUserRepository userRepository;
    private final BusinessEventPublisher eventPublisher;

    public UserService(AppUserRepository userRepository, BusinessEventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public AppUser updateNotificationPreferences(UUID userId, boolean emailEnabled, boolean inAppEnabled) {
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthenticationRequiredException("User no longer exists."));
        user.updateNotificationPreferences(emailEnabled, inAppEnabled);
        eventPublisher.publish(new BusinessEvent(
                null,
                EventType.USER_UPDATED,
                EventOutcome.SUCCESS,
                EntityType.USER,
                user.getId().toString(),
                user.getId().toString(),
                null,
                EventMetadata.of("field", "notificationPreferences")
        ));
        return user;
    }
}
