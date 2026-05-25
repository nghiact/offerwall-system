package com.ctn.offerwall.offer.category;

import com.ctn.offerwall.offer.category.dto.OfferCategoryRequest;
import com.ctn.offerwall.offer.category.dto.OfferCategoryResponse;
import com.ctn.offerwall.offer.domain.OfferCategory;
import com.ctn.offerwall.offer.exception.CategoryInUseException;
import com.ctn.offerwall.offer.exception.CategoryNotFoundException;
import com.ctn.offerwall.offer.exception.DuplicateCategoryCodeException;
import com.ctn.offerwall.offer.repository.OfferCategoryRepository;
import com.ctn.offerwall.offer.repository.OfferRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class OfferCategoryService {

    private final OfferCategoryRepository categoryRepository;
    private final OfferRepository offerRepository;

    public OfferCategoryService(OfferCategoryRepository categoryRepository, OfferRepository offerRepository) {
        this.categoryRepository = categoryRepository;
        this.offerRepository = offerRepository;
    }

    @Transactional(readOnly = true)
    public List<OfferCategoryResponse> listCategories() {
        return categoryRepository.findAll(Sort.by("name").ascending()).stream()
                .map(OfferCategoryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public OfferCategoryResponse getCategory(UUID id) {
        return OfferCategoryResponse.from(findCategory(id));
    }

    @Transactional
    public OfferCategoryResponse createCategory(OfferCategoryRequest request) {
        String code = normalizeCode(request.code());
        validateCodeAvailable(code, null);

        OfferCategory category = new OfferCategory(
                code,
                request.name().trim(),
                trimToNull(request.description())
        );
        return OfferCategoryResponse.from(categoryRepository.save(category));
    }

    @Transactional
    public OfferCategoryResponse updateCategory(UUID id, OfferCategoryRequest request) {
        OfferCategory category = findCategory(id);
        String code = normalizeCode(request.code());
        validateCodeAvailable(code, id);

        category.update(
                code,
                request.name().trim(),
                trimToNull(request.description())
        );
        return OfferCategoryResponse.from(category);
    }

    @Transactional
    public void deleteCategory(UUID id) {
        OfferCategory category = findCategory(id);
        if (offerRepository.existsByCategoryId(id)) {
            throw new CategoryInUseException("Offer category is in use.");
        }
        categoryRepository.delete(category);
    }

    private OfferCategory findCategory(UUID id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException("Offer category was not found."));
    }

    private String normalizeCode(String code) {
        return code.trim().toLowerCase(Locale.ROOT);
    }

    private void validateCodeAvailable(String code, UUID currentCategoryId) {
        categoryRepository.findByCodeIgnoreCase(code)
                .filter(existing -> !existing.getId().equals(currentCategoryId))
                .ifPresent(existing -> {
                    throw new DuplicateCategoryCodeException("Offer category code already exists.");
                });
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
