package com.ctn.offerwall.common.card;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BinRulesTests {

    @Test
    void acceptsStoredBinLengthFromSixToTwelveDigits() {
        assertThat(BinRules.isValidBin("123456")).isTrue();
        assertThat(BinRules.isValidBin("123456789012")).isTrue();
    }

    @Test
    void rejectsInvalidStoredBins() {
        assertThat(BinRules.isValidBin("12345")).isFalse();
        assertThat(BinRules.isValidBin("1234567890123")).isFalse();
        assertThat(BinRules.isValidBin("12345a")).isFalse();
        assertThat(BinRules.isValidBin(null)).isFalse();
    }

    @Test
    void acceptsLookupPrefixLengthFromFourToEightDigits() {
        assertThat(BinRules.isValidLookupPrefix("1234")).isTrue();
        assertThat(BinRules.isValidLookupPrefix("12345678")).isTrue();
    }

    @Test
    void rejectsInvalidLookupPrefixes() {
        assertThat(BinRules.isValidLookupPrefix("123")).isFalse();
        assertThat(BinRules.isValidLookupPrefix("123456789")).isFalse();
        assertThat(BinRules.isValidLookupPrefix("123x")).isFalse();
        assertThat(BinRules.isValidLookupPrefix(null)).isFalse();
    }
}
