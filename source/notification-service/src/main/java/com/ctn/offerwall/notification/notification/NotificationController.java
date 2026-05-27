package com.ctn.offerwall.notification.notification;

import com.ctn.offerwall.notification.notification.dto.InAppNotificationResponse;
import com.ctn.offerwall.notification.notification.dto.NotificationCampaignResponse;
import com.ctn.offerwall.notification.notification.dto.NotificationRequest;
import com.ctn.offerwall.notification.security.NotificationSecurityService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final NotificationSecurityService securityService;

    public NotificationController(NotificationService notificationService,
                                  NotificationSecurityService securityService) {
        this.notificationService = notificationService;
        this.securityService = securityService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public NotificationCampaignResponse create(
            @RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestHeader(name = NotificationSecurityService.INTERNAL_SERVICE_KEY_HEADER, required = false) String internalServiceKey,
            @Valid @RequestBody NotificationRequest request) {
        securityService.requireEditorOrAdminOrInternal(authorization, internalServiceKey);
        return notificationService.createNotification(request);
    }

    @GetMapping
    public List<NotificationCampaignResponse> list(
            @RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestHeader(name = NotificationSecurityService.INTERNAL_SERVICE_KEY_HEADER, required = false) String internalServiceKey) {
        securityService.requireEditorOrAdminOrInternal(authorization, internalServiceKey);
        return notificationService.listCampaigns();
    }

    @GetMapping("/{id}")
    public NotificationCampaignResponse get(
            @RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestHeader(name = NotificationSecurityService.INTERNAL_SERVICE_KEY_HEADER, required = false) String internalServiceKey,
            @PathVariable UUID id) {
        securityService.requireEditorOrAdminOrInternal(authorization, internalServiceKey);
        return notificationService.getCampaign(id);
    }

    @GetMapping("/users/{userId}/in-app")
    public List<InAppNotificationResponse> getInAppNotifications(
            @RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @PathVariable UUID userId) {
        securityService.requireUser(authorization, userId);
        return notificationService.getInAppNotifications(userId);
    }

    @PatchMapping("/in-app/{deliveryId}/read")
    public InAppNotificationResponse markInAppRead(
            @RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @PathVariable UUID deliveryId) {
        UUID requesterUserId = securityService.requireAuthenticatedUser(authorization).userId();
        return notificationService.markInAppRead(deliveryId, requesterUserId);
    }
}
