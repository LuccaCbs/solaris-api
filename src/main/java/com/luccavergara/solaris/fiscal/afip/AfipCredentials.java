package com.luccavergara.solaris.fiscal.afip;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.util.StringUtils;

import java.util.Optional;

/**
 * AFIP native credentials stored in {@code organizations.fiscal_api_key} as JSON:
 * {@code {"provider":"afip_native","cuit":"...","puntoVenta":1,"certPath":"...","certPassword":"..."}}
 * Global defaults from {@code afip.*} properties apply when fields are omitted.
 */
public record AfipCredentials(
        String cuit,
        Integer puntoVenta,
        String certPath,
        String certPassword,
        String certBase64
) {

    public boolean hasCertificateReference() {
        return StringUtils.hasText(certPath)
                || StringUtils.hasText(certBase64);
    }

    public static Optional<AfipCredentials> parse(String fiscalApiKey, ObjectMapper objectMapper) {
        if (!StringUtils.hasText(fiscalApiKey)) {
            return Optional.of(empty());
        }

        String trimmed = fiscalApiKey.trim();
        if (!trimmed.startsWith("{")) {
            return Optional.empty();
        }

        try {
            JsonNode root = objectMapper.readTree(trimmed);

            if (root.has("apikey") || root.has("apitoken") || root.has("usertoken")) {
                return Optional.empty();
            }

            String provider = textValue(root, "provider");
            if (StringUtils.hasText(provider)
                    && !"afip_native".equalsIgnoreCase(provider)
                    && !"AFIP_NATIVE".equalsIgnoreCase(provider)) {
                return Optional.empty();
            }

            AfipCredentials credentials = new AfipCredentials(
                    textValue(root, "cuit"),
                    intValue(root, "puntoVenta"),
                    textValue(root, "certPath"),
                    textValue(root, "certPassword"),
                    textValue(root, "certBase64")
            );

            return Optional.of(credentials);
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    public static AfipCredentials empty() {
        return new AfipCredentials(null, null, null, null, null);
    }

    private static String textValue(JsonNode root, String fieldName) {
        JsonNode node = root.get(fieldName);
        if (node == null || node.isNull()) {
            return null;
        }
        String value = node.asText(null);
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private static Integer intValue(JsonNode root, String fieldName) {
        JsonNode node = root.get(fieldName);
        if (node == null || node.isNull() || !node.isNumber()) {
            return null;
        }
        return node.intValue();
    }
}
