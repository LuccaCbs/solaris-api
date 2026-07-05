package com.luccavergara.solaris.util;

import org.springframework.util.StringUtils;

public final class SpainTaxIdValidator {

    private SpainTaxIdValidator() {
    }

    public static String normalize(String taxId) {
        if (!StringUtils.hasText(taxId)) {
            return null;
        }

        return taxId.trim().toUpperCase().replaceAll("[\\s-]", "");
    }

    public static boolean isValid(String taxId) {
        String normalized = normalize(taxId);

        if (!StringUtils.hasText(normalized)) {
            return false;
        }

        return normalized.matches("^[0-9A-Z]{8,9}$");
    }
}
