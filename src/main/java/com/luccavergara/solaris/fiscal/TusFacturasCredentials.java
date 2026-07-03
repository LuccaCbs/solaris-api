package com.luccavergara.solaris.fiscal;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.util.StringUtils;

import java.util.Optional;

/**
 * TusFacturas requires three credentials per organization. They are stored as JSON
 * in {@code organizations.fiscal_api_key}:
 * {@code {"apikey":"...","apitoken":"...","usertoken":"..."}}.
 */
public record TusFacturasCredentials(String apiKey, String apiToken, String userToken) {

    public boolean isComplete() {
        return StringUtils.hasText(apiKey)
                && StringUtils.hasText(apiToken)
                && StringUtils.hasText(userToken);
    }

    public static Optional<TusFacturasCredentials> parse(String fiscalApiKey, ObjectMapper objectMapper) {
        if (!StringUtils.hasText(fiscalApiKey)) {
            return Optional.empty();
        }

        String trimmed = fiscalApiKey.trim();
        if (!trimmed.startsWith("{")) {
            return Optional.empty();
        }

        try {
            JsonNode root = objectMapper.readTree(trimmed);
            TusFacturasCredentials credentials = new TusFacturasCredentials(
                    textValue(root, "apikey"),
                    textValue(root, "apitoken"),
                    textValue(root, "usertoken")
            );
            return credentials.isComplete() ? Optional.of(credentials) : Optional.empty();
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    private static String textValue(JsonNode root, String fieldName) {
        JsonNode node = root.get(fieldName);
        if (node == null || node.isNull()) {
            return null;
        }
        String value = node.asText(null);
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
