package com.ctn.offerwall.offer.offer;

import com.ctn.offerwall.common.offer.OfferEligibilityMode;
import com.ctn.offerwall.common.offer.OfferType;
import com.ctn.offerwall.offer.domain.OfferStatus;
import com.ctn.offerwall.offer.offer.dto.OfferRequest;
import com.ctn.offerwall.offer.offer.dto.OfferResponse;
import com.ctn.offerwall.offer.security.OfferSecurityService;
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
@RequestMapping("/api/offers")
public class OfferController {

    private final OfferService offerService;
    private final OfferSecurityService securityService;

    public OfferController(OfferService offerService, OfferSecurityService securityService) {
        this.offerService = offerService;
        this.securityService = securityService;
    }

    @GetMapping
    public List<OfferResponse> list(@RequestParam(required = false) UUID categoryId,
                                    @RequestParam(required = false) OfferType type,
                                    @RequestParam(required = false) OfferEligibilityMode eligibilityMode,
                                    @RequestParam(required = false) OfferStatus status,
                                    @RequestParam(required = false) String keyword) {
        return offerService.listOffers(categoryId, type, eligibilityMode, status, keyword);
    }

    @GetMapping("/{id}")
    public OfferResponse get(@PathVariable UUID id) {
        return offerService.getOffer(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OfferResponse create(@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorization,
                                @Valid @RequestBody OfferRequest request) {
        String actorUserId = securityService.requireEditorOrAdmin(authorization);
        return offerService.createOffer(request, actorUserId);
    }

    @PutMapping("/{id}")
    public OfferResponse update(@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorization,
                                @PathVariable UUID id,
                                @Valid @RequestBody OfferRequest request) {
        securityService.requireEditorOrAdmin(authorization);
        return offerService.updateOffer(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorization,
                       @PathVariable UUID id) {
        securityService.requireEditorOrAdmin(authorization);
        offerService.deleteOffer(id);
    }
}
