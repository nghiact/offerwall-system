package com.ctn.offerwall.card.card;

import com.ctn.offerwall.card.card.dto.CardProductLookupRequest;
import com.ctn.offerwall.card.card.dto.CardProductSummaryResponse;
import com.ctn.offerwall.card.security.InternalSecurityService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/internal/card-products")
public class CardProductLookupController {

    private final CardService cardService;
    private final InternalSecurityService securityService;

    public CardProductLookupController(CardService cardService, InternalSecurityService securityService) {
        this.cardService = cardService;
        this.securityService = securityService;
    }

    @PostMapping("/lookup")
    public List<CardProductSummaryResponse> lookup(
            @RequestHeader(name = InternalSecurityService.INTERNAL_SERVICE_KEY_HEADER, required = false) String internalServiceKey,
            @Valid @RequestBody CardProductLookupRequest request) {
        securityService.requireInternalServiceKey(internalServiceKey);
        return cardService.lookupCardSummaries(request.cardProductIds());
    }
}
