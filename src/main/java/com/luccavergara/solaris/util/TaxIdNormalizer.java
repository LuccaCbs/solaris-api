package com.luccavergara.solaris.util;

import com.luccavergara.solaris.entity.DocumentType;
import org.springframework.util.StringUtils;

public final class TaxIdNormalizer {

    private TaxIdNormalizer() {
    }

    public static String normalizeCuit(String cuit) {
        if (!StringUtils.hasText(cuit)) {
            return null;
        }

        return cuit.replaceAll("\\D", "");
    }

    public static String normalizeDocumentNumber(DocumentType documentType, String documentNumber) {
        if (!StringUtils.hasText(documentNumber)) {
            return null;
        }

        String normalized = documentNumber.trim();

        if (documentType == DocumentType.CUIT || documentType == DocumentType.CUIL) {
            normalized = normalizeCuit(normalized);
        } else {
            normalized = normalized.replaceAll("\\D", "");
        }

        return normalized;
    }

    public static void validateDocumentNumber(DocumentType documentType, String documentNumber) {
        if (!StringUtils.hasText(documentNumber)) {
            throw new IllegalArgumentException("Document number is required");
        }

        String normalized = normalizeDocumentNumber(documentType, documentNumber);

        if (documentType == DocumentType.CUIT || documentType == DocumentType.CUIL) {
            if (normalized == null || !normalized.matches("\\d{11}")) {
                throw new IllegalArgumentException("CUIT/CUIL must contain 11 digits");
            }
            return;
        }

        if (normalized == null || !normalized.matches("\\d{7,8}")) {
            throw new IllegalArgumentException("DNI must contain 7 or 8 digits");
        }
    }

    public static String normalizePhone(String phone) {
        if (!StringUtils.hasText(phone)) {
            return null;
        }

        String normalized = phone.replaceAll("\\s", "").trim();

        return normalized.isEmpty() ? null : normalized;
    }

    public static void validatePhone(String phone) {
        if (phone == null) {
            return;
        }

        if (!phone.matches("^\\+?[1-9]\\d{7,14}$")) {
            throw new IllegalArgumentException("Phone number must be in international format");
        }
    }

    public static String normalizeEmail(String email) {
        if (!StringUtils.hasText(email)) {
            return null;
        }

        return email.trim();
    }
}
