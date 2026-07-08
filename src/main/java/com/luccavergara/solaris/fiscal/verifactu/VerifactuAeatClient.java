package com.luccavergara.solaris.fiscal.verifactu;

import com.luccavergara.solaris.entity.TipoComprobante;
import com.luccavergara.solaris.fiscal.EmitCreditNoteCommand;
import com.luccavergara.solaris.fiscal.EmitInvoiceCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
@RequiredArgsConstructor
public class VerifactuAeatClient {

    private static final String SOAP_ACTION = VerifactuWsdlEndpoints.SOAP_ACTION;
    private static final DateTimeFormatter EXPEDITION_DATE = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private final VerifactuProperties verifactuProperties;
    private final VerifactuEndpointResolver endpointResolver;
    private final VerifactuHashCalculator hashCalculator;
    private final VerifactuXmlBuilder xmlBuilder;
    private final VerifactuRecordSigner recordSigner;
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
        VerifactuXmlBuilder.VerifactuSubmission submission = VerifactuXmlBuilder.VerifactuSubmission.forAlta(
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

        String registroFragment = xmlBuilder.buildRegistroAltaFragment(submission);
        String envelope = buildSignedEnvelope(
                credentials,
                nif,
                emitterRazonSocial,
                registroFragment
        );

        String qrUrl = qrUrlBuilder.buildValidationUrl(
                verifactuProperties,
                endpointResolver,
                nif,
                numSerieFactura,
                fechaExpedicion,
                command.getImporteTotal(),
                huella
        );

        return dispatch(
                command.getTipoComprobante(),
                command.getPuntoVenta(),
                command.getNumeroComprobante(),
                huella,
                qrUrl,
                envelope,
                credentials
        );
    }

    public VerifactuInvoiceAuthorization submitRectificativa(
            EmitCreditNoteCommand command,
            VerifactuCredentials credentials,
            String nif,
            String emitterRazonSocial,
            String previousHash
    ) {
        String numSerieFactura = buildCreditNoteNumSerieFactura(command, credentials);
        String relatedNumSerieFactura = resolveRelatedNumSerieFactura(command, credentials);
        String relatedFechaExpedicion = resolveRelatedFechaExpedicion(command);
        LocalDate fechaExpedicion = LocalDate.now();
        String tipoFactura = command.getRectificationKind().code();
        VerifactuCorrectionType correctionType = command.getCorrectionType() != null
                ? command.getCorrectionType()
                : VerifactuCorrectionType.I;
        String fechaHora = VerifactuXmlBuilder.VerifactuSubmission.nowIsoSpain();

        EmitInvoiceCommand invoiceCommand = EmitInvoiceCommand.builder()
                .emitterCuit(command.getEmitterCuit())
                .emitterRazonSocial(command.getEmitterRazonSocial())
                .puntoVenta(command.getPuntoVenta())
                .tipoComprobante(command.getTipoComprobante())
                .numeroComprobante(command.getNumeroComprobante())
                .importeNeto(command.getImporteNeto())
                .importeIva(command.getImporteIva())
                .importeTotal(command.getImporteTotal())
                .build();

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
        VerifactuXmlBuilder.VerifactuSubmission submission = VerifactuXmlBuilder.VerifactuSubmission.forRectificativa(
                invoiceCommand,
                nif,
                emitterRazonSocial,
                numSerieFactura,
                fechaExpedicion,
                tipoFactura,
                correctionType.code(),
                relatedNumSerieFactura,
                relatedFechaExpedicion,
                command.getCorrectedBaseAmount(),
                command.getCorrectedTaxAmount(),
                "Rectificativa registrada en Solaris",
                huella,
                fechaHora,
                emitterRazonSocial,
                resolveSoftwareName(credentials),
                resolveSoftwareId(credentials),
                resolveSoftwareVersion(credentials),
                resolveInstallationNumber(credentials)
        );

        String registroFragment = xmlBuilder.buildRegistroAltaFragment(submission);
        String envelope = buildSignedEnvelope(
                credentials,
                nif,
                emitterRazonSocial,
                registroFragment
        );

        String qrUrl = qrUrlBuilder.buildValidationUrl(
                verifactuProperties,
                endpointResolver,
                nif,
                numSerieFactura,
                fechaExpedicion,
                command.getImporteTotal(),
                huella
        );

        TipoComprobante tipo = command.getTipoComprobante() != null
                ? command.getTipoComprobante()
                : TipoComprobante.FACTURA_B;

        return dispatch(
                tipo,
                command.getPuntoVenta(),
                command.getNumeroComprobante(),
                huella,
                qrUrl,
                envelope,
                credentials
        );
    }

    public VerifactuInvoiceAuthorization submitAnulacion(
            EmitCreditNoteCommand command,
            VerifactuCredentials credentials,
            String nif,
            String emitterRazonSocial,
            String previousHash
    ) {
        String numSerieFacturaAnulada = resolveRelatedNumSerieFactura(command, credentials);
        String fechaExpedicionAnulada = resolveRelatedFechaExpedicion(command);
        String fechaHora = VerifactuXmlBuilder.VerifactuSubmission.nowIsoSpain();

        VerifactuHashCalculator.VerifactuAnulacionRecord hashInput =
                new VerifactuHashCalculator.VerifactuAnulacionRecord(
                        nif,
                        numSerieFacturaAnulada,
                        fechaExpedicionAnulada,
                        previousHash == null ? "" : previousHash,
                        fechaHora
                );

        String huella = hashCalculator.calculateAnulacionFingerprint(hashInput);
        VerifactuXmlBuilder.VerifactuAnulacionSubmission submission = new VerifactuXmlBuilder.VerifactuAnulacionSubmission(
                nif,
                emitterRazonSocial,
                numSerieFacturaAnulada,
                fechaExpedicionAnulada,
                huella,
                fechaHora,
                emitterRazonSocial,
                resolveSoftwareName(credentials),
                resolveSoftwareId(credentials),
                resolveSoftwareVersion(credentials),
                resolveInstallationNumber(credentials)
        );

        String registroFragment = xmlBuilder.buildRegistroAnulacionFragment(submission);
        String envelope = buildSignedEnvelope(
                credentials,
                nif,
                emitterRazonSocial,
                registroFragment
        );

        TipoComprobante tipo = command.getTipoComprobante() != null
                ? command.getTipoComprobante()
                : TipoComprobante.FACTURA_B;

        return dispatch(
                tipo,
                command.getPuntoVenta(),
                command.getNumeroComprobante() != null ? command.getNumeroComprobante() : command.getRelatedInvoiceNumero(),
                huella,
                null,
                envelope,
                credentials
        );
    }

    private String buildSignedEnvelope(
            VerifactuCredentials credentials,
            String nif,
            String emitterRazonSocial,
            String registroFragment
    ) {
        if (!verifactuProperties.getSignature().isEnabled()) {
            return xmlBuilder.buildRegFactuEnvelope(nif, emitterRazonSocial, registroFragment);
        }

        VerifactuCertificateLoader.VerifactuKeyMaterial keyMaterial = certificateLoader.load(credentials);
        String signedRegistro = recordSigner.sign(registroFragment, keyMaterial);
        return xmlBuilder.buildRegFactuEnvelope(nif, emitterRazonSocial, signedRegistro);
    }

    private VerifactuInvoiceAuthorization dispatch(
            TipoComprobante tipoComprobante,
            Integer puntoVenta,
            Long numeroComprobante,
            String huella,
            String qrUrl,
            String envelope,
            VerifactuCredentials credentials
    ) {
        if (!endpointResolver.isSubmissionEnabled(verifactuProperties)) {
            return VerifactuInvoiceAuthorization.rejected(
                    tipoComprobante,
                    puntoVenta,
                    numeroComprobante,
                    huella,
                    qrUrl,
                    envelope,
                    "Verifactu is disabled (enable verifactu.sandbox.enabled or verifactu.production.enabled)"
            );
        }

        try {
            VerifactuCertificateLoader.VerifactuKeyMaterial keyMaterial = certificateLoader.load(credentials);
            String response = httpTransport.post(
                    endpointResolver.resolveServiceUrl(verifactuProperties),
                    SOAP_ACTION,
                    envelope,
                    keyMaterial
            );

            return parseResponse(tipoComprobante, puntoVenta, numeroComprobante, huella, qrUrl, envelope, response);
        } catch (Exception ex) {
            return VerifactuInvoiceAuthorization.rejected(
                    tipoComprobante,
                    puntoVenta,
                    numeroComprobante,
                    huella,
                    qrUrl,
                    envelope,
                    "Verifactu request failed: " + ex.getMessage()
            );
        }
    }

    private VerifactuInvoiceAuthorization parseResponse(
            TipoComprobante tipoComprobante,
            Integer puntoVenta,
            Long numeroComprobante,
            String huella,
            String qrUrl,
            String requestXml,
            String responseXml
    ) {
        String fault = xmlHelper.firstFault(responseXml);
        if (StringUtils.hasText(fault)) {
            return VerifactuInvoiceAuthorization.rejected(
                    tipoComprobante,
                    puntoVenta,
                    numeroComprobante,
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
                    tipoComprobante,
                    puntoVenta,
                    numeroComprobante,
                    huella,
                    qrUrl,
                    requestXml,
                    reason
            );
        }

        return VerifactuInvoiceAuthorization.authorized(
                tipoComprobante,
                puntoVenta,
                numeroComprobante,
                huella,
                qrUrl,
                requestXml,
                responseXml
        );
    }

    private String buildCreditNoteNumSerieFactura(EmitCreditNoteCommand command, VerifactuCredentials credentials) {
        int serie = credentials.serie() != null
                ? credentials.serie()
                : command.getPuntoVenta() != null ? command.getPuntoVenta() : verifactuProperties.getSerie();
        long numero = command.getNumeroComprobante() != null ? command.getNumeroComprobante() : 1L;
        return serie + "-" + numero;
    }

    private String resolveRelatedNumSerieFactura(EmitCreditNoteCommand command, VerifactuCredentials credentials) {
        if (StringUtils.hasText(command.getRelatedNumSerieFactura())) {
            return command.getRelatedNumSerieFactura().trim();
        }

        int serie = credentials.serie() != null
                ? credentials.serie()
                : command.getPuntoVenta() != null ? command.getPuntoVenta() : verifactuProperties.getSerie();
        long numero = command.getRelatedInvoiceNumero() != null ? command.getRelatedInvoiceNumero() : 1L;
        return serie + "-" + numero;
    }

    private String resolveRelatedFechaExpedicion(EmitCreditNoteCommand command) {
        if (StringUtils.hasText(command.getRelatedFechaExpedicion())) {
            return command.getRelatedFechaExpedicion().trim();
        }
        return formatDate(LocalDate.now());
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
        return EXPEDITION_DATE.format(date);
    }

    private String formatAmount(java.math.BigDecimal amount) {
        if (amount == null) {
            return "0";
        }
        return amount.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }
}
