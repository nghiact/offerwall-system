package com.ctn.offerwall.card.card;

import com.ctn.offerwall.card.card.dto.CardProductRequest;
import com.ctn.offerwall.card.card.dto.CardProductResponse;
import com.ctn.offerwall.card.security.CardSecurityService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/cards")
public class CardController {

    private final CardService cardService;
    private final CardSecurityService securityService;

    public CardController(CardService cardService, CardSecurityService securityService) {
        this.cardService = cardService;
        this.securityService = securityService;
    }

    @GetMapping
    public List<CardProductResponse> list(@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        securityService.requireAuthenticated(authorization);
        return cardService.listCards();
    }

    @GetMapping("/{id}")
    public CardProductResponse get(@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorization,
                                   @PathVariable UUID id) {
        securityService.requireAuthenticated(authorization);
        return cardService.getCard(id);
    }

    @GetMapping("/matches")
    public List<CardProductResponse> match(@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorization,
                                           @RequestParam String prefix) {
        securityService.requireAuthenticated(authorization);
        return cardService.matchByPrefix(prefix);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CardProductResponse create(@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorization,
                                      @Valid @RequestBody CardProductRequest request) {
        securityService.requireAuthenticated(authorization);
        return cardService.createCard(request);
    }

    @PutMapping("/{id}")
    public CardProductResponse update(@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorization,
                                      @PathVariable UUID id,
                                      @Valid @RequestBody CardProductRequest request) {
        securityService.requireAuthenticated(authorization);
        return cardService.updateCard(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorization,
                       @PathVariable UUID id) {
        securityService.requireAuthenticated(authorization);
        cardService.deleteCard(id);
    }
}
