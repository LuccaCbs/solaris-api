package com.luccavergara.solaris.fiscal.verifactu;

import com.luccavergara.solaris.entity.TipoComprobante;
import com.luccavergara.solaris.fiscal.EmitInvoiceCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class VerifactuAeatClient {

    private static final String SOAP_ACTION =
            "https://www2.agenciatributaria.gob.es/static_files/common/internet/dep/aplicaciones/es/aeat/tike/cont/ws/SistemaFacturacion.wsdl/RegFactuSistemaFacturacion";

    private final VerifactuProperties verifactuProperties;
    private final VerifactuHashCalculator hashCalculator;
    private final VerifactuXmlBuilder xmlBuilder;
    private final VerifactuHttpTransport httpTransport;
    private final VerifactuCertificateLoader certificateLoader;
    private final VerifactuXmlHelper xmlHelper;
    private final VerifactuQrUrlBuilder qrUrlBuilder;

    public VerifactuInvoiceAuthorization submitAlta(
            EmitInvoiceCommand command,
            VerifactuCredentials credentials,
            String nif,
            String emitterRazonSocial,
            String previousHash
    ) {
        String numSerieFactura = buildNumSerieFactura(command, credentials);
        LocalDate fechaExpedicion = LocalDate.now();
        String tipoFactura = resolveTipoFactura(command);
        String fechaHora = VerifactuXmlBuilder.VerifactuSubmission.nowIsoSpain();

        VerifactuHashCalculator.VerifactuAltaRecord hashInput = new VerifactuHashCalculator.VerifactuAltaRecord(
                nif,
                numSerieFactura,
                formatDate(fechaExpedicion),
                tipoFactura,
                formatAmount(command.getImporteIva()),
                formatAmount(command.getImporteTotal()),
                previousHash == null ? "" : previousHash,
                fechaHora
        );

        String huella = hashCalculator.calculateAltaFingerprint(hashInput);
        VerifactuXmlBuilder.VerifactuSubmission submission = new VerifactuXmlBuilder.VerifactuSubmission(
                command,
                nif,
                emitterRazonSocial,
                numSerieFactura,
                fechaExpedicion,
                tipoFactura,
                "Venta registrada en Solaris",
                huella,
                fechaHora,
                emitterRazonSocial,
                resolveSoftwareName(credentials),
                resolveSoftwareId(credentials),
                resolveSoftwareVersion(credentials),
                resolveInstallationNumber(credentials)
        );

        String envelope = xmlBuilder.buildRegFactuEnvelope(submission);
        String qrUrl = qrUrlBuilder.buildValidationUrl(
                verifactuProperties,
                nif,
                numSerieFactura,
                fechaExpedicion,
                command.getImporteTotal(),
                huella
        );

        if (!verifactuProperties.getSandbox().isEnabled()) {
            return VerifactuInvoiceAuthorization.rejected(
                    command.getTipoComprobante(),
                    command.getPuntoVenta(),
                    command.getNumeroComprobante(),
                    huella,
                    qrUrl,
                    envelope,
                    "Verifactu sandbox is disabled (set verifactu.sandbox.enabled=true)"
            );
        }

        try {
            VerifactuCertificateLoader.VerifactuKeyMaterial keyMaterial = certificateLoader.load(credentials);
            String response = httpTransport.post(
                    verifactuProperties.getSandbox().getServiceUrl(),
                    SOAP_ACTION,
                    envelope,
                    keyMaterial
            );

            return parseResponse(command, huella, qrUrl, envelope, response);
        } catch (Exception ex) {
            return VerifactuInvoiceAuthorization.rejected(
                    command.getTipoComprobante(),
                    command.getPuntoVenta(),
                    command.getNumeroComprobante(),
                    huella,
                    qrUrl,
                    envelope,
                    "Verifactu request failed: " + ex.getMessage()
            );
        }
    }

    private VerifactuInvoiceAuthorization parseResponse(
            EmitInvoiceCommand command,
            String huella,
            String qrUrl,
            String requestXml,
            String responseXml
    ) {
        String fault = xmlHelper.firstFault(responseXml);
        if (StringUtils.hasText(fault)) {
            return VerifactuInvoiceAuthorization.rejected(
                    command.getTipoComprobante(),
                    command.getPuntoVenta(),
                    command.getNumeroComprobante(),
                    huella,
                    qrUrl,
                    requestXml,
                    fault
            );
        }

        String estadoEnvio = xmlHelper.textContent(responseXml, "EstadoEnvio");
        String estadoRegistro = xmlHelper.textContent(responseXml, "EstadoRegistro");
        String codigoError = xmlHelper.textContent(responseXml, "CodigoErrorRegistro");
        String descripcionError = xmlHelper.textContent(responseXml, "DescripcionErrorRegistro");

        boolean authorized = "Correcto".equalsIgnoreCase(estadoEnvio)
                || "Correcto".equalsIgnoreCase(estadoRegistro)
                || "AceptadoConErrores".equalsIgnoreCase(estadoEnvio)
                || "AceptadoConErrores".equalsIgnoreCase(estadoRegistro);

        if (!authorized) {
            String reason = StringUtils.hasText(descripcionError)
                    ? descripcionError
                    : "Verifactu rejected submission (EstadoEnvio=" + estadoEnvio + ", EstadoRegistro=" + estadoRegistro + ")";
            if (StringUtils.hasText(codigoError)) {
                reason = codigoError + ": " + reason;
            }

            return VerifactuInvoiceAuthorization.rejected(
                    command.getTipoComprobante(),
                    command.getPuntoVenta(),
                    command.getNumeroComprobante(),
                    huella,
                    qrUrl,
                    requestXml,
                    reason
            );
        }

        return VerifactuInvoiceAuthorization.authorized(
                command.getTipoComprobante(),
                command.getPuntoVenta(),
                command.getNumeroComprobante(),
                huella,
                qrUrl,
                requestXml,
                responseXml
        );
    }

    private String resolveTipoFactura(EmitInvoiceCommand command) {
        if (command.getCustomerDocumentNumber() == null || "0".equals(command.getCustomerDocumentNumber())) {
            return "F2";
        }
        return "F1";
    }

    private String buildNumSerieFactura(EmitInvoiceCommand command, VerifactuCredentials credentials) {
        int serie = credentials.serie() != null
                ? credentials.serie()
                : command.getPuntoVenta() != null ? command.getPuntoVenta() : verifactuProperties.getSerie();
        long numero = command.getNumeroComprobante() != null ? command.getNumeroComprobante() : 1L;
        return serie + "-" + numero;
    }

    private String resolveSoftwareName(VerifactuCredentials credentials) {
        if (credentials != null && StringUtils.hasText(credentials.softwareName())) {
            return credentials.softwareName();
        }
        return verifactuProperties.getSoftware().getName();
    }

    private String resolveSoftwareId(VerifactuCredentials credentials) {
        if (credentials != null && StringUtils.hasText(credentials.softwareId())) {
            return credentials.softwareId();
        }
        return verifactuProperties.getSoftware().getId();
    }

    private String resolveSoftwareVersion(VerifactuCredentials credentials) {
        if (credentials != null && StringUtils.hasText(credentials.softwareVersion())) {
            return credentials.softwareVersion();
        }
        return verifactuProperties.getSoftware().getVersion();
    }

    private String resolveInstallationNumber(VerifactuCredentials credentials) {
        if (credentials != null && StringUtils.hasText(credentials.installationNumber())) {
            return credentials.installationNumber();
        }
        return verifactuProperties.getSoftware().getInstallationNumber();
    }

    private String formatDate(LocalDate date) {
        return String.format("%02d-%02d-%04d", date.getDayOfMonth(), date.getMonthValue(), date.getYear());
    }

    private String formatAmount(java.math.BigDecimal amount) {
        if (amount == null) {
            return "0";
        }
        return amount.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }
}
