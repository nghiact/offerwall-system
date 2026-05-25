package com.ctn.offerwall.user.notification;

import com.ctn.offerwall.user.notification.dto.NotificationRecipientResponse;
import com.ctn.offerwall.user.notification.dto.OfferNotificationRecipientRequest;
import com.ctn.offerwall.user.security.InternalSecurityService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/internal/notification-recipients")
public class NotificationRecipientController {

    private final NotificationRecipientService recipientService;
    private final InternalSecurityService securityService;

    public NotificationRecipientController(NotificationRecipientService recipientService,
                                           InternalSecurityService securityService) {
        this.recipientService = recipientService;
        this.securityService = securityService;
    }

    @PostMapping("/offers")
    public List<NotificationRecipientResponse> resolveOfferRecipients(
            @RequestHeader(name = InternalSecurityService.INTERNAL_SERVICE_KEY_HEADER, required = false) String internalServiceKey,
            @Valid @RequestBody OfferNotificationRecipientRequest request) {
        securityService.requireInternalServiceKey(internalServiceKey);
        return recipientService.resolveOfferRecipients(request);
    }
}
