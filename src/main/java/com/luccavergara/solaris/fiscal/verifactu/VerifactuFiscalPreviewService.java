package com.luccavergara.solaris.fiscal.verifactu;

import com.luccavergara.solaris.entity.Organization;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Builds a local Verifactu fiscal representation preview without contacting AEAT.
 * Useful to validate HTML layout and QR URL format before sandbox integration.
 */
@Service
@RequiredArgsConstructor
public class VerifactuFiscalPreviewService {

    private final VerifactuProperties verifactuProperties;
    private final VerifactuEndpointResolver endpointResolver;
    private final VerifactuQrUrlBuilder qrUrlBuilder;
    private final VerifactuFiscalRepresentationBuilder fiscalRepresentationBuilder;
    private final VerifactuSoftwareDeclarationService softwareDeclarationService;
    private final VerifactuHashCalculator hashCalculator;

    public String buildPreviewHtml(Organization organization) {
        String nif = resolveNif(organization);
        String razonSocial = StringUtils.hasText(organization.getRazonSocial())
                ? organization.getRazonSocial()
                : "Empresa de prueba";
        int serie = organization.getFiscalPuntoVenta() != null
                ? organization.getFiscalPuntoVenta()
                : verifactuProperties.getSerie();
        long numero = 999L;
        String numSerie = serie + "-" + numero;
        LocalDate fecha = LocalDate.now();
        BigDecimal importeNeto = new BigDecimal("100.00");
        BigDecimal importeIva = new BigDecimal("21.00");
        BigDecimal importeTotal = new BigDecimal("121.00");
        String fechaHora = VerifactuXmlBuilder.VerifactuSubmission.nowIsoSpain();

        String huella = hashCalculator.calculateAltaFingerprint(
                new VerifactuHashCalculator.VerifactuAltaRecord(
                        nif,
                        numSerie,
                        formatDate(fecha),
                        "F2",
                        "21.00",
                        "121.00",
                        "",
                        fechaHora
                )
        );

        String qrUrl = qrUrlBuilder.buildValidationUrl(
                endpointResolver.resolveQrValidationBaseUrl(verifactuProperties),
                nif,
                numSerie,
                fecha,
                importeTotal,
                huella
        );

        return fiscalRepresentationBuilder.buildHtml(
                nif,
                razonSocial,
                numSerie,
                fecha,
                importeTotal,
                huella,
                qrUrl,
                "F2 (preview)",
                softwareDeclarationService.build(verifactuProperties)
        );
    }

    public VerifactuPreviewMetadata buildPreviewMetadata(Organization organization) {
        String html = buildPreviewHtml(organization);
        return new VerifactuPreviewMetadata(
                endpointResolver.resolveEnvironmentLabel(verifactuProperties),
                endpointResolver.resolveQrValidationBaseUrl(verifactuProperties),
                endpointResolver.resolveServiceUrl(verifactuProperties),
                html.length(),
                html
        );
    }

    private String resolveNif(Organization organization) {
        if (StringUtils.hasText(organization.getCuit())) {
            return organization.getCuit().trim().toUpperCase();
        }
        if (StringUtils.hasText(verifactuProperties.getNif())) {
            return verifactuProperties.getNif().trim().toUpperCase();
        }
        return "B12345678";
    }

    private String formatDate(LocalDate date) {
        return String.format("%02d-%02d-%04d", date.getDayOfMonth(), date.getMonthValue(), date.getYear());
    }

    public record VerifactuPreviewMetadata(
            String environment,
            String qrValidationBaseUrl,
            String soapServiceUrl,
            int htmlLength,
            String html
    ) {
    }
}
