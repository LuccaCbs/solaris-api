package com.luccavergara.solaris.fiscal.verifactu;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.util.StringUtils;

import java.util.Optional;

/**
 * Verifactu credentials stored in {@code organizations.fiscal_api_key} as JSON:
 * {@code {"provider":"verifactu_native","nif":"B12345678","serie":1,"certBase64":"...","certPassword":"..."}}
 */
public record VerifactuCredentials(
        String nif,
        Integer serie,
        String certPath,
        String certPassword,
        String certBase64,
        String softwareName,
        String softwareId,
        String softwareVersion,
        String installationNumber
) {

    public boolean hasCertificateReference() {
        return StringUtils.hasText(certPath) || StringUtils.hasText(certBase64);
    }

    public static Optional<VerifactuCredentials> parse(String fiscalApiKey, ObjectMapper objectMapper) {
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
                    && !"verifactu_native".equalsIgnoreCase(provider)
                    && !"VERIFACTU_NATIVE".equalsIgnoreCase(provider)) {
                return Optional.empty();
            }

            return Optional.of(new VerifactuCredentials(
                    firstText(root, "nif", "cuit"),
                    intValue(root, "serie", "puntoVenta"),
                    textValue(root, "certPath"),
                    textValue(root, "certPassword"),
                    textValue(root, "certBase64"),
                    textValue(root, "softwareName"),
                    textValue(root, "softwareId"),
                    textValue(root, "softwareVersion"),
                    textValue(root, "installationNumber")
            ));
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    public static VerifactuCredentials empty() {
        return new VerifactuCredentials(null, null, null, null, null, null, null, null, null);
    }

    private static String firstText(JsonNode root, String... fieldNames) {
        for (String fieldName : fieldNames) {
            String value = textValue(root, fieldName);
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }

    private static String textValue(JsonNode root, String fieldName) {
        JsonNode node = root.get(fieldName);
        if (node == null || node.isNull()) {
            return null;
        }
        String value = node.asText(null);
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private static Integer intValue(JsonNode root, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode node = root.get(fieldName);
            if (node != null && !node.isNull() && node.isNumber()) {
                return node.intValue();
            }
        }
        return null;
    }
}
