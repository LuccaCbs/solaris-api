package com.luccavergara.solaris.util;

import com.luccavergara.solaris.entity.BarcodeFormat;

public final class BarcodeUtils {

    private static final String INTERNAL_EAN_PREFIX = "779999";

    private BarcodeUtils() {
    }

    public static BarcodeFormat detectFormat(String barcode) {
        if (barcode == null || barcode.isBlank()) {
            throw new IllegalArgumentException("Barcode is required");
        }

        String normalized = barcode.trim();

        if (normalized.matches("\\d{13}") && isValidEan13(normalized)) {
            return BarcodeFormat.EAN_13;
        }

        if (normalized.matches("\\d{12}") && isValidUpcA(normalized)) {
            return BarcodeFormat.UPC_A;
        }

        if (normalized.matches("[0-9A-Z\\-. $/+%*]+") && normalized.matches(".*[A-Z].*")) {
            return BarcodeFormat.CODE_39;
        }

        return BarcodeFormat.CODE_128;
    }

    public static void validateBarcode(String barcode, BarcodeFormat format) {
        if (barcode == null || barcode.isBlank()) {
            throw new IllegalArgumentException("Barcode is required");
        }

        String normalized = barcode.trim();

        switch (format) {
            case EAN_13 -> {
                if (!normalized.matches("\\d{13}")) {
                    throw new IllegalArgumentException("EAN-13 must contain exactly 13 digits");
                }
                if (!isValidEan13(normalized)) {
                    throw new IllegalArgumentException("Invalid EAN-13 check digit");
                }
            }
            case UPC_A -> {
                if (!normalized.matches("\\d{12}")) {
                    throw new IllegalArgumentException("UPC-A must contain exactly 12 digits");
                }
                if (!isValidUpcA(normalized)) {
                    throw new IllegalArgumentException("Invalid UPC-A check digit");
                }
            }
            case CODE_39 -> {
                if (!normalized.matches("[0-9A-Z\\-. $/+%]+")) {
                    throw new IllegalArgumentException("CODE-39 allows uppercase letters, digits and - . $ / + % space");
                }
            }
            case CODE_128 -> {
                if (normalized.length() > 80) {
                    throw new IllegalArgumentException("CODE-128 barcode is too long");
                }
            }
            default -> throw new IllegalArgumentException("Unsupported barcode format");
        }
    }

    public static String generateInternalEan13(int sequence) {
        if (sequence < 1 || sequence > 99999) {
            throw new IllegalArgumentException("Barcode sequence out of range");
        }

        String base = INTERNAL_EAN_PREFIX + String.format("%05d", sequence);
        return base + calculateEan13CheckDigit(base);
    }

    public static boolean isValidEan13(String barcode) {
        if (barcode == null || !barcode.matches("\\d{13}")) {
            return false;
        }

        int expected = Character.getNumericValue(barcode.charAt(12));
        String base = barcode.substring(0, 12);
        return expected == calculateEan13CheckDigit(base);
    }

    public static boolean isValidUpcA(String barcode) {
        if (barcode == null || !barcode.matches("\\d{12}")) {
            return false;
        }

        int expected = Character.getNumericValue(barcode.charAt(11));
        return expected == calculateUpcCheckDigit(barcode.substring(0, 11));
    }

    private static int calculateEan13CheckDigit(String first12Digits) {
        int sum = 0;

        for (int index = 0; index < 12; index++) {
            int digit = Character.getNumericValue(first12Digits.charAt(index));
            sum += index % 2 == 0 ? digit : digit * 3;
        }

        return (10 - (sum % 10)) % 10;
    }

    private static int calculateUpcCheckDigit(String first11Digits) {
        int sum = 0;

        for (int index = 0; index < 11; index++) {
            int digit = Character.getNumericValue(first11Digits.charAt(index));
            sum += index % 2 == 0 ? digit * 3 : digit;
        }

        return (10 - (sum % 10)) % 10;
    }
}
