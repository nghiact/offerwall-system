package com.ctn.offerwall.common.card;

import java.util.regex.Pattern;

public final class BinRules {

    public static final int MIN_BIN_LENGTH = 6;
    public static final int MAX_BIN_LENGTH = 12;
    public static final int MIN_LOOKUP_PREFIX_LENGTH = 4;
    public static final int MAX_LOOKUP_PREFIX_LENGTH = 8;

    private static final Pattern BIN_PATTERN = Pattern.compile("\\d{6,12}");
    private static final Pattern LOOKUP_PREFIX_PATTERN = Pattern.compile("\\d{4,8}");

    private BinRules() {
    }

    public static boolean isValidBin(String value) {
        return value != null && BIN_PATTERN.matcher(value).matches();
    }

    public static boolean isValidLookupPrefix(String value) {
        return value != null && LOOKUP_PREFIX_PATTERN.matcher(value).matches();
    }
}
