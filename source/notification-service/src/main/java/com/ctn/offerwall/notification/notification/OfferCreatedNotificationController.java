package com.ctn.offerwall.notification.notification;

import com.ctn.offerwall.notification.notification.dto.NotificationCampaignResponse;
import com.ctn.offerwall.notification.notification.dto.OfferCreatedNotificationRequest;
import com.ctn.offerwall.notification.security.NotificationSecurityService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/notifications/offer-created-events")
public class OfferCreatedNotificationController {

    private final OfferCreatedNotificationService notificationService;
    private final NotificationSecurityService securityService;

    public OfferCreatedNotificationController(OfferCreatedNotificationService notificationService,
                                              NotificationSecurityService securityService) {
        this.notificationService = notificationService;
        this.securityService = securityService;
    }

    @PostMapping("/{eventId}")
    public NotificationCampaignResponse createFromOfferCreatedEvent(
            @RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestHeader(name = NotificationSecurityService.INTERNAL_SERVICE_KEY_HEADER, required = false) String internalServiceKey,
            @PathVariable UUID eventId,
            @Valid @RequestBody OfferCreatedNotificationRequest request) {
        securityService.requireEditorOrAdminOrInternal(authorization, internalServiceKey);
        return notificationService.createFromOfferCreatedEvent(eventId, request);
    }
}
