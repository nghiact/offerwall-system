package com.ctn.offerwall.card.card;

import com.ctn.offerwall.card.domain.CardProduct;
import com.ctn.offerwall.common.card.CardNetwork;
import com.ctn.offerwall.common.card.CardType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CardDisplayFormatterTests {

    private final CardDisplayFormatter formatter = new CardDisplayFormatter();

    @Test
    void formatsNetworkSpecificTierName() {
        CardProduct card = new CardProduct("vcb-vibe", "Vietcombank", "Vibe", CardNetwork.VISA, 2, null, CardType.CREDIT, true, java.util.List.of("123456"));

        assertThat(formatter.displayName(card)).isEqualTo("Vietcombank Vibe Visa Platinum credit card");
    }

    @Test
    void usesTierLabelOverride() {
        CardProduct card = new CardProduct("localbank-napas", "Localbank", null, CardNetwork.NAPAS, 0, "Domestic", CardType.DEBIT, true, java.util.List.of("123456"));

        assertThat(formatter.displayName(card)).isEqualTo("Localbank Napas Domestic debit card");
    }

    @Test
    void formatsNapasTierLabels() {
        CardProduct card = new CardProduct("localbank-napas", "Localbank", null, CardNetwork.NAPAS, 2, null, CardType.DEBIT, true, java.util.List.of("123456"));

        assertThat(formatter.displayName(card)).isEqualTo("Localbank Napas Gold debit card");
    }
}
