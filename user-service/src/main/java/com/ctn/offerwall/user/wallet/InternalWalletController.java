package com.ctn.offerwall.user.wallet;

import com.ctn.offerwall.user.eligibility.dto.UserWalletCandidate;
import com.ctn.offerwall.user.security.InternalSecurityService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/internal/users/{userId}/wallet")
public class InternalWalletController {

    private final WalletService walletService;
    private final InternalSecurityService securityService;

    public InternalWalletController(WalletService walletService, InternalSecurityService securityService) {
        this.walletService = walletService;
        this.securityService = securityService;
    }

    @GetMapping("/candidate")
    public UserWalletCandidate candidate(
            @RequestHeader(name = InternalSecurityService.INTERNAL_SERVICE_KEY_HEADER, required = false) String internalServiceKey,
            @PathVariable UUID userId) {
        securityService.requireInternalServiceKey(internalServiceKey);
        return walletService.walletCandidate(userId);
    }
}
