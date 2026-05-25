package com.ctn.offerwall.user.wallet;

import com.ctn.offerwall.user.auth.AuthService;
import com.ctn.offerwall.user.domain.AppUser;
import com.ctn.offerwall.user.wallet.dto.WalletCardRequest;
import com.ctn.offerwall.user.wallet.dto.WalletCardResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/users/me/wallet/cards")
public class WalletController {

    private final AuthService authService;
    private final WalletService walletService;

    public WalletController(AuthService authService, WalletService walletService) {
        this.authService = authService;
        this.walletService = walletService;
    }

    @GetMapping
    public List<WalletCardResponse> list(@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        AppUser user = authService.requireBearerUser(authorization);
        return walletService.listCards(user.getId());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WalletCardResponse add(@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorization,
                                  @Valid @RequestBody WalletCardRequest request) {
        AppUser user = authService.requireBearerUser(authorization);
        return walletService.addCard(user.getId(), request.cardProductId());
    }

    @DeleteMapping("/{walletCardId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorization,
                       @PathVariable UUID walletCardId) {
        AppUser user = authService.requireBearerUser(authorization);
        walletService.deleteCard(user.getId(), walletCardId);
    }
}
