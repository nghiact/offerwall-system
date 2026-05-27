package com.ctn.offerwall.offer.seed;

import com.ctn.offerwall.common.card.CardNetwork;
import com.ctn.offerwall.common.card.CardType;
import com.ctn.offerwall.common.offer.OfferEligibilityMode;
import com.ctn.offerwall.common.offer.OfferType;
import com.ctn.offerwall.offer.config.SeedProperties;
import com.ctn.offerwall.offer.domain.Offer;
import com.ctn.offerwall.offer.domain.OfferCategory;
import com.ctn.offerwall.offer.repository.OfferCategoryRepository;
import com.ctn.offerwall.offer.repository.OfferRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
public class LocalSeedRunner implements CommandLineRunner {

    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final SeedProperties properties;
    private final Environment environment;
    private final OfferCategoryRepository categoryRepository;
    private final OfferRepository offerRepository;

    public LocalSeedRunner(SeedProperties properties,
                           Environment environment,
                           OfferCategoryRepository categoryRepository,
                           OfferRepository offerRepository) {
        this.properties = properties;
        this.environment = environment;
        this.categoryRepository = categoryRepository;
        this.offerRepository = offerRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!properties.isEnabled() || !hasLocalSeedProfile()) {
            return;
        }

        OfferCategory shopping = seedCategory("shopping", "Shopping", "Local POC shopping offers.");
        OfferCategory foodAndBeverages = seedCategory(
                "food-beverages",
                "Food & Beverages",
                "Local POC food and beverage offers."
        );

        seedOffer(new OfferSeed(
                shopping,
                "Uniqlo",
                "[Kanshasai Week Summer 2026] Receive VND300k back for bill from VND3m",
                "https://map.uniqlo.com/vn/vi/",
                "https://map.uniqlo.com/vn/vi/",
                LocalDate.of(2026, 5, 22),
                LocalDate.of(2026, 5, 28),
                OfferType.BOTH,
                List.of(),
                List.of(CardNetwork.JCB),
                null,
                List.of(),
                null
        ));
        seedOffer(new OfferSeed(
                foodAndBeverages,
                "Golden Gate",
                "Get VND300k off for bill from VND1.5m",
                "17 locations nationwide",
                null,
                LocalDate.of(2026, 4, 17),
                LocalDate.of(2026, 7, 31),
                OfferType.OFFLINE,
                List.of("MB"),
                List.of(CardNetwork.VISA),
                2,
                List.of(CardType.CREDIT),
                null
        ));
    }

    private OfferCategory seedCategory(String code, String name, String description) {
        return categoryRepository.findByCodeIgnoreCase(code)
                .map(existing -> {
                    existing.update(code, name, description);
                    return existing;
                })
                .orElseGet(() -> categoryRepository.save(new OfferCategory(code, name, description)));
    }

    private void seedOffer(OfferSeed seed) {
        offerRepository.findByMerchantNameIgnoreCaseAndOfferSummary(seed.merchantName(), seed.summary())
                .ifPresentOrElse(
                        existing -> existing.update(
                                seed.category(),
                                seed.merchantName(),
                                seed.summary(),
                                seed.addressDisplay(),
                                seed.addressUrl(),
                                seed.startDate().atStartOfDay(VIETNAM_ZONE).toInstant(),
                                seed.endDate().plusDays(1).atStartOfDay(VIETNAM_ZONE).toInstant(),
                                seed.offerType(),
                                OfferEligibilityMode.CRITERIA,
                                Set.of(),
                                new LinkedHashSet<>(seed.targetIssuers()),
                                new LinkedHashSet<>(seed.targetNetworks()),
                                seed.targetTier(),
                                new LinkedHashSet<>(seed.targetTypes()),
                                seed.targetPersonal()
                        ),
                        () -> offerRepository.save(new Offer(
                                seed.category(),
                                seed.merchantName(),
                                seed.summary(),
                                seed.addressDisplay(),
                                seed.addressUrl(),
                                seed.startDate().atStartOfDay(VIETNAM_ZONE).toInstant(),
                                seed.endDate().plusDays(1).atStartOfDay(VIETNAM_ZONE).toInstant(),
                                seed.offerType(),
                                OfferEligibilityMode.CRITERIA,
                                Set.of(),
                                new LinkedHashSet<>(seed.targetIssuers()),
                                new LinkedHashSet<>(seed.targetNetworks()),
                                seed.targetTier(),
                                new LinkedHashSet<>(seed.targetTypes()),
                                seed.targetPersonal()
                        ))
                );
    }

    private boolean hasLocalSeedProfile() {
        return List.of(environment.getActiveProfiles()).stream()
                .map(profile -> profile.toLowerCase(Locale.ROOT))
                .anyMatch("local-seed"::equals);
    }

    private record OfferSeed(OfferCategory category,
                             String merchantName,
                             String summary,
                             String addressDisplay,
                             String addressUrl,
                             LocalDate startDate,
                             LocalDate endDate,
                             OfferType offerType,
                             List<String> targetIssuers,
                             List<CardNetwork> targetNetworks,
                             Integer targetTier,
                             List<CardType> targetTypes,
                             Boolean targetPersonal) {
    }
}
