package com.ctn.offerwall.card.card;

import com.ctn.offerwall.card.card.dto.CardProductRequest;
import com.ctn.offerwall.card.card.dto.CardProductResponse;
import com.ctn.offerwall.card.domain.CardBin;
import com.ctn.offerwall.card.domain.CardProduct;
import com.ctn.offerwall.card.exception.CardNotFoundException;
import com.ctn.offerwall.card.exception.DuplicateProductCodeException;
import com.ctn.offerwall.card.exception.UserInputException;
import com.ctn.offerwall.card.repository.CardBinRepository;
import com.ctn.offerwall.card.repository.CardProductRepository;
import com.ctn.offerwall.common.card.BinRules;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class CardService {

    private final CardProductRepository productRepository;
    private final CardBinRepository binRepository;
    private final CardDisplayFormatter displayFormatter;

    public CardService(CardProductRepository productRepository,
                       CardBinRepository binRepository,
                       CardDisplayFormatter displayFormatter) {
        this.productRepository = productRepository;
        this.binRepository = binRepository;
        this.displayFormatter = displayFormatter;
    }

    @Transactional(readOnly = true)
    public List<CardProductResponse> listCards() {
        return productRepository.findAllWithBins().stream()
                .map(product -> toResponse(product, List.of()))
                .toList();
    }

    @Transactional(readOnly = true)
    public CardProductResponse getCard(UUID id) {
        return toResponse(findProduct(id), List.of());
    }

    @Transactional(readOnly = true)
    public List<CardProductResponse> matchByPrefix(String prefix) {
        if (!BinRules.isValidLookupPrefix(prefix)) {
            throw new UserInputException("Card lookup prefix must be 4 to 8 digits.");
        }

        Map<UUID, ProductMatch> matchesByProduct = new LinkedHashMap<>();
        for (CardBin bin : binRepository.findMatchesForPrefix(prefix)) {
            CardProduct product = bin.getCardProduct();
            ProductMatch match = matchesByProduct.computeIfAbsent(product.getId(), ignored -> new ProductMatch(product));
            match.matchedBins().add(bin.getBin());
        }

        return matchesByProduct.values().stream()
                .map(match -> toResponse(match.product(), match.matchedBins()))
                .toList();
    }

    @Transactional
    public CardProductResponse createCard(CardProductRequest request) {
        String productCode = normalizeProductCode(request.productCode());
        validateProductCodeAvailable(productCode, null);
        List<String> bins = validateBins(request.bins());

        CardProduct product = new CardProduct(
                productCode,
                request.issuer().trim(),
                trimToNull(request.name()),
                request.network(),
                request.tier(),
                trimToNull(request.tierLabelOverride()),
                request.type(),
                personalOrDefault(request.personal()),
                bins
        );
        return toResponse(productRepository.save(product), List.of());
    }

    @Transactional
    public CardProductResponse updateCard(UUID id, CardProductRequest request) {
        String productCode = normalizeProductCode(request.productCode());
        CardProduct product = findProduct(id);
        validateProductCodeAvailable(productCode, id);
        List<String> bins = validateBins(request.bins());

        product.update(
                productCode,
                request.issuer().trim(),
                trimToNull(request.name()),
                request.network(),
                request.tier(),
                trimToNull(request.tierLabelOverride()),
                request.type(),
                personalOrDefault(request.personal()),
                bins
        );
        return toResponse(product, List.of());
    }

    @Transactional
    public void deleteCard(UUID id) {
        CardProduct product = findProduct(id);
        productRepository.delete(product);
    }

    private CardProduct findProduct(UUID id) {
        return productRepository.findByIdWithBins(id)
                .orElseThrow(() -> new CardNotFoundException("Card product was not found."));
    }

    private CardProductResponse toResponse(CardProduct product, List<String> matchedBins) {
        return CardProductResponse.from(
                product,
                displayFormatter.tierLabel(product),
                displayFormatter.displayName(product),
                matchedBins
        );
    }

    private List<String> validateBins(List<String> bins) {
        if (bins == null || bins.isEmpty()) {
            throw new UserInputException("Card product must have at least one BIN.");
        }

        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String bin : bins) {
            if (!BinRules.isValidBin(bin)) {
                throw new UserInputException("Card BIN must be 6 to 12 digits.");
            }
            normalized.add(bin);
        }
        if (normalized.size() != bins.size()) {
            throw new UserInputException("Card product cannot contain duplicate BINs.");
        }
        return new ArrayList<>(normalized);
    }

    private String normalizeProductCode(String productCode) {
        return productCode.trim().toLowerCase(Locale.ROOT);
    }

    private void validateProductCodeAvailable(String productCode, UUID currentProductId) {
        productRepository.findByProductCodeIgnoreCase(productCode)
                .filter(existing -> !existing.getId().equals(currentProductId))
                .ifPresent(existing -> {
                    throw new DuplicateProductCodeException("Card product code already exists.");
                });
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private boolean personalOrDefault(Boolean personal) {
        return personal == null || personal;
    }

    private record ProductMatch(CardProduct product, List<String> matchedBins) {

        private ProductMatch(CardProduct product) {
            this(product, new ArrayList<>());
        }
    }
}
