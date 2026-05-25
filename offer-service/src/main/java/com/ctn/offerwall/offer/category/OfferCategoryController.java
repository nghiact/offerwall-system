package com.ctn.offerwall.offer.category;

import com.ctn.offerwall.offer.category.dto.OfferCategoryRequest;
import com.ctn.offerwall.offer.category.dto.OfferCategoryResponse;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/offer-categories")
public class OfferCategoryController {

    private final OfferCategoryService categoryService;
    private final OfferSecurityService securityService;

    public OfferCategoryController(OfferCategoryService categoryService, OfferSecurityService securityService) {
        this.categoryService = categoryService;
        this.securityService = securityService;
    }

    @GetMapping
    public List<OfferCategoryResponse> list() {
        return categoryService.listCategories();
    }

    @GetMapping("/{id}")
    public OfferCategoryResponse get(@PathVariable UUID id) {
        return categoryService.getCategory(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OfferCategoryResponse create(@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorization,
                                        @Valid @RequestBody OfferCategoryRequest request) {
        securityService.requireEditorOrAdmin(authorization);
        return categoryService.createCategory(request);
    }

    @PutMapping("/{id}")
    public OfferCategoryResponse update(@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorization,
                                        @PathVariable UUID id,
                                        @Valid @RequestBody OfferCategoryRequest request) {
        securityService.requireEditorOrAdmin(authorization);
        return categoryService.updateCategory(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorization,
                       @PathVariable UUID id) {
        securityService.requireEditorOrAdmin(authorization);
        categoryService.deleteCategory(id);
    }
}
