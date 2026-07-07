package com.luccavergara.solaris.fiscal.verifactu;

import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
public class VerifactuQrUrlBuilder {

    private static final DateTimeFormatter QR_DATE = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    public String buildValidationUrl(
            VerifactuProperties properties,
            String nif,
            String numSerieFactura,
            LocalDate fechaExpedicion,
            BigDecimal importeTotal,
            String huella
    ) {
        return UriComponentsBuilder
                .fromUriString(properties.getSandbox().getQrValidationBaseUrl())
                .queryParam("nif", encode(nif))
                .queryParam("numserie", encode(numSerieFactura))
                .queryParam("fecha", encode(QR_DATE.format(fechaExpedicion)))
                .queryParam("importe", encode(formatAmount(importeTotal)))
                .queryParam("huella", encode(huella))
                .build(true)
                .toUriString();
    }

    private String formatAmount(BigDecimal amount) {
        return amount.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
