package com.luccavergara.solaris.fiscal.verifactu;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
public class VerifactuFiscalRepresentationBuilder {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public String buildHtml(
            String nif,
            String emitterRazonSocial,
            String numSerieFactura,
            LocalDate fechaExpedicion,
            BigDecimal importeTotal,
            String huella,
            String qrUrl,
            String tipoFactura,
            VerifactuSoftwareDeclarationService.VerifactuSoftwareDeclaration declaration
    ) {
        return """
                <!DOCTYPE html>
                <html lang="es">
                <head>
                  <meta charset="UTF-8"/>
                  <title>Factura Verifactu %s</title>
                  <style>
                    body { font-family: Arial, sans-serif; margin: 2rem; color: #111; }
                    h1 { font-size: 1.25rem; margin-bottom: 0.5rem; }
                    .meta { margin: 0.25rem 0; }
                    .qr { margin-top: 1.5rem; }
                    .declaration { margin-top: 2rem; font-size: 0.85rem; color: #444; white-space: pre-wrap; }
                  </style>
                </head>
                <body>
                  <h1>Representación fiscal Verifactu</h1>
                  <p class="meta"><strong>Emisor:</strong> %s (%s)</p>
                  <p class="meta"><strong>Factura:</strong> %s</p>
                  <p class="meta"><strong>Tipo:</strong> %s</p>
                  <p class="meta"><strong>Fecha expedición:</strong> %s</p>
                  <p class="meta"><strong>Importe total:</strong> %s €</p>
                  <p class="meta"><strong>Huella:</strong> %s</p>
                  <div class="qr">
                    <p><strong>Validación AEAT:</strong></p>
                    <p><a href="%s">%s</a></p>
                  </div>
                  %s
                </body>
                </html>
                """.formatted(
                escape(numSerieFactura),
                escape(emitterRazonSocial),
                escape(nif),
                escape(numSerieFactura),
                escape(tipoFactura),
                DATE.format(fechaExpedicion),
                formatAmount(importeTotal),
                escape(huella),
                escape(qrUrl),
                escape(qrUrl),
                buildDeclarationSection(declaration)
        );
    }

    private String buildDeclarationSection(VerifactuSoftwareDeclarationService.VerifactuSoftwareDeclaration declaration) {
        if (declaration == null) {
            return "";
        }

        String urlBlock = StringUtils.hasText(declaration.declarationUrl())
                ? "<p><a href=\"" + escape(declaration.declarationUrl()) + "\">Declaración responsable (documento)</a></p>"
                : "";

        return """
                <div class="declaration">
                  <strong>Declaración responsable del SIF</strong>
                  %s
                  <p>%s</p>
                </div>
                """.formatted(urlBlock, escape(declaration.declarationText()));
    }

    private String formatAmount(BigDecimal amount) {
        if (amount == null) {
            return "0.00";
        }
        return amount.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
