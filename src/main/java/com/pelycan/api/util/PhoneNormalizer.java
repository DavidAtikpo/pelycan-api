package com.pelycan.api.util;

public final class PhoneNormalizer {

    private PhoneNormalizer() {
    }

    public static String normalize(String phone, String defaultCountryCode) {
        if (phone == null || phone.isBlank()) {
            return null;
        }
        String digits = phone.replaceAll("[^0-9+]", "");
        if (digits.isEmpty()) {
            return null;
        }
        if (digits.startsWith("+")) {
            return digits;
        }
        if (digits.startsWith("00")) {
            return "+" + digits.substring(2);
        }
        String country = defaultCountryCode != null && !defaultCountryCode.isBlank()
                ? defaultCountryCode.replaceAll("[^0-9]", "")
                : "33";
        if (digits.startsWith("0")) {
            return "+" + country + digits.substring(1);
        }
        return "+" + country + digits;
    }
}
