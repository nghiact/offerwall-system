package com.ctn.offerwall.card.card;

import com.ctn.offerwall.card.domain.CardProduct;
import com.ctn.offerwall.common.card.CardNetwork;
import com.ctn.offerwall.common.card.CardType;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class CardDisplayFormatter {

    private static final Map<CardNetwork, String[]> TIER_LABELS = createTierLabels();

    public String displayName(CardProduct card) {
        return Stream.of(
                        card.getIssuer(),
                        card.getName(),
                        networkLabel(card.getNetwork()),
                        tierLabel(card),
                        typeLabel(card.getType()),
                        "card"
                )
                .filter(value -> value != null && !value.isBlank())
                .collect(Collectors.joining(" "));
    }

    public String tierLabel(CardProduct card) {
        if (card.getTierLabelOverride() != null && !card.getTierLabelOverride().isBlank()) {
            return card.getTierLabelOverride();
        }

        String[] labels = TIER_LABELS.get(card.getNetwork());
        if (labels == null || card.getTier() < 0 || card.getTier() >= labels.length) {
            return "Tier " + card.getTier();
        }
        return labels[card.getTier()];
    }

    private String networkLabel(CardNetwork network) {
        return switch (network) {
            case VISA -> "Visa";
            case MASTERCARD -> "Mastercard";
            case JCB -> "JCB";
            case UNIONPAY -> "UnionPay";
            case AMEX -> "AmEx";
            case NAPAS -> "Napas";
        };
    }

    private String typeLabel(CardType type) {
        return switch (type) {
            case CREDIT -> "credit";
            case DEBIT -> "debit";
            case PREPAID -> "prepaid";
            case HYBRID -> "hybrid";
        };
    }

    private static Map<CardNetwork, String[]> createTierLabels() {
        Map<CardNetwork, String[]> labels = new EnumMap<>(CardNetwork.class);
        labels.put(CardNetwork.VISA, new String[]{"Classic", "Gold", "Platinum", "Signature", "Infinite", "Infinite Privilege"});
        labels.put(CardNetwork.MASTERCARD, new String[]{"Standard", "Gold", "Platinum", "World", "World Elite", "World Champion"});
        labels.put(CardNetwork.JCB, new String[]{"Standard", "Gold", "Platinum", "Ultimate", "World Elite", "The Class"});
        labels.put(CardNetwork.UNIONPAY, new String[]{"Standard", "Gold", "Platinum", "Diamond", "Infinite", "Infinite Privilege"});
        labels.put(CardNetwork.AMEX, new String[]{"Green", "Gold", "Platinum", "Platinum", "Centurion", "Centurion Black"});
        labels.put(CardNetwork.NAPAS, new String[]{"", "Standard", "Gold", "Platinum", "", ""});
        return labels;
    }
}
