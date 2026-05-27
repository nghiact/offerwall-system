package com.ctn.offerwall.card.seed;

import com.ctn.offerwall.card.card.CardDisplayFormatter;
import com.ctn.offerwall.card.config.SeedProperties;
import com.ctn.offerwall.card.domain.CardProduct;
import com.ctn.offerwall.card.repository.CardProductRepository;
import com.ctn.offerwall.common.card.CardNetwork;
import com.ctn.offerwall.common.card.CardType;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class LocalSeedRunner implements CommandLineRunner {

    private final SeedProperties properties;
    private final Environment environment;
    private final CardProductRepository productRepository;
    private final CardDisplayFormatter displayFormatter;

    public LocalSeedRunner(SeedProperties properties,
                           Environment environment,
                           CardProductRepository productRepository,
                           CardDisplayFormatter displayFormatter) {
        this.properties = properties;
        this.environment = environment;
        this.productRepository = productRepository;
        this.displayFormatter = displayFormatter;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!properties.isEnabled() || !hasLocalSeedProfile()) {
            return;
        }

        seedCard(new CardSeed("35677255", "MB", CardNetwork.JCB, 4, CardType.CREDIT, true));
        seedCard(new CardSeed("41581500", "VPBank", CardNetwork.VISA, 1, CardType.DEBIT, true));
        seedCard(new CardSeed("534146", "Agribank", CardNetwork.MASTERCARD, 2, CardType.CREDIT, true));
        seedCard(new CardSeed("420417", "MB", CardNetwork.VISA, 5, CardType.CREDIT, true));
    }

    private void seedCard(CardSeed seed) {
        CardProduct draft = new CardProduct(
                "draft",
                seed.issuer(),
                null,
                seed.network(),
                seed.tier(),
                null,
                seed.type(),
                seed.personal(),
                List.of(seed.bin())
        );
        String productCode = displayFormatter.displayName(draft);

        productRepository.findByProductCodeIgnoreCase(productCode)
                .ifPresentOrElse(
                        existing -> existing.update(
                                productCode,
                                seed.issuer(),
                                null,
                                seed.network(),
                                seed.tier(),
                                null,
                                seed.type(),
                                seed.personal(),
                                List.of(seed.bin())
                        ),
                        () -> productRepository.save(new CardProduct(
                                productCode,
                                seed.issuer(),
                                null,
                                seed.network(),
                                seed.tier(),
                                null,
                                seed.type(),
                                seed.personal(),
                                List.of(seed.bin())
                        ))
                );
    }

    private boolean hasLocalSeedProfile() {
        return List.of(environment.getActiveProfiles()).contains("local-seed");
    }

    private record CardSeed(String bin,
                            String issuer,
                            CardNetwork network,
                            int tier,
                            CardType type,
                            boolean personal) {
    }
}
