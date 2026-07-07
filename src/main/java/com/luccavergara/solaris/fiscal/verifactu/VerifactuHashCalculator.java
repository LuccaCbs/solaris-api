package com.luccavergara.solaris.fiscal.verifactu;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AEAT Verifactu huella (SHA-256) for registro de facturación de alta.
 * Field order and format follow the public AEAT hash specification.
 */
@Component
public class VerifactuHashCalculator {

    public String calculateAltaFingerprint(VerifactuAltaRecord record) {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("IDEmisorFactura", normalize(record.idEmisorFactura()));
        fields.put("NumSerieFactura", normalize(record.numSerieFactura()));
        fields.put("FechaExpedicionFactura", normalize(record.fechaExpedicionFactura()));
        fields.put("TipoFactura", normalize(record.tipoFactura()));
        fields.put("CuotaTotal", normalizeAmount(record.cuotaTotal()));
        fields.put("ImporteTotal", normalizeAmount(record.importeTotal()));
        fields.put("Huella", normalize(record.huellaAnterior()));
        fields.put("FechaHoraHusoGenRegistro", normalize(record.fechaHoraHusoGenRegistro()));

        String payload = fields.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("&"));

        return sha256Hex(payload);
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.replace(" ", "");
    }

    private String normalizeAmount(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String trimmed = value.trim();
        if (!trimmed.contains(".")) {
            return trimmed;
        }

        String normalized = trimmed.replaceAll("0+$", "").replaceAll("\\.$", "");
        return normalized.isEmpty() ? "0" : normalized;
    }

    private String sha256Hex(String payload) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte value : hash) {
                builder.append(String.format("%02X", value));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }

    public record VerifactuAltaRecord(
            String idEmisorFactura,
            String numSerieFactura,
            String fechaExpedicionFactura,
            String tipoFactura,
            String cuotaTotal,
            String importeTotal,
            String huellaAnterior,
            String fechaHoraHusoGenRegistro
    ) {
    }
}
