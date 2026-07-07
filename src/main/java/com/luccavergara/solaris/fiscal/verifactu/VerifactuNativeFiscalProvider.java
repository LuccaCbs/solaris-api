package com.luccavergara.solaris.fiscal.verifactu;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.luccavergara.solaris.fiscal.EmitCreditNoteCommand;
import com.luccavergara.solaris.fiscal.EmitInvoiceCommand;
import com.luccavergara.solaris.fiscal.EmitInvoiceResult;
import com.luccavergara.solaris.fiscal.FiscalProvider;
import com.luccavergara.solaris.util.SpainTaxIdValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class VerifactuNativeFiscalProvider implements FiscalProvider {

    private final VerifactuProperties verifactuProperties;
    private final VerifactuAeatClient aeatClient;
    private final VerifactuHashChainService hashChainService;
    private final ObjectMapper objectMapper;

    @Override
    public EmitInvoiceResult emitInvoice(EmitInvoiceCommand command) {
        throw new IllegalStateException(
                "Verifactu native provider requires credentials; use emitInvoice(command, credentials, organizationId)"
        );
    }

    public EmitInvoiceResult emitInvoice(
            EmitInvoiceCommand command,
            VerifactuCredentials credentials,
            Long organizationId
    ) {
        String nif = resolveNif(command.getEmitterCuit(), credentials);
        if (!StringUtils.hasText(nif)) {
            return rejectedInvoice(command, "Verifactu NIF/CIF is required (organization or credentials)", null);
        }

        if (!SpainTaxIdValidator.isValid(nif)) {
            return rejectedInvoice(command, "Verifactu NIF/CIF format is invalid", null);
        }

        String emitterRazonSocial = command.getEmitterRazonSocial();
        if (!StringUtils.hasText(emitterRazonSocial)) {
            return rejectedInvoice(command, "Business name (razon social) is required for Verifactu", null);
        }

        try {
            String previousHash = hashChainService.resolvePreviousHash(organizationId);
            VerifactuInvoiceAuthorization authorization = aeatClient.submitAlta(
                    command,
                    credentials,
                    nif,
                    emitterRazonSocial,
                    previousHash
            );

            if (!authorization.isAuthorized()) {
                return rejectedInvoice(command, authorization.getRejectionReason(), authorization.getRequestXml());
            }

            return toAuthorizedResult(authorization, "alta");
        } catch (Exception ex) {
            log.error("Verifactu native invoice emission failed: {}", ex.getMessage());
            return rejectedInvoice(command, "Verifactu request failed: " + ex.getMessage(), null);
        }
    }

    @Override
    public EmitInvoiceResult emitCreditNote(EmitCreditNoteCommand command) {
        throw new IllegalStateException(
                "Verifactu native provider requires credentials; use emitCreditNote(command, credentials, organizationId)"
        );
    }

    public EmitInvoiceResult emitCreditNote(
            EmitCreditNoteCommand command,
            VerifactuCredentials credentials,
            Long organizationId
    ) {
        String nif = resolveNif(command.getEmitterCuit(), credentials);
        if (!StringUtils.hasText(nif)) {
            return rejectedCreditNote(command, "Verifactu NIF/CIF is required (organization or credentials)", null);
        }

        if (!SpainTaxIdValidator.isValid(nif)) {
            return rejectedCreditNote(command, "Verifactu NIF/CIF format is invalid", null);
        }

        if (command.getRelatedInvoiceNumero() == null && !StringUtils.hasText(command.getRelatedNumSerieFactura())) {
            return rejectedCreditNote(command, "Related invoice reference is required for Verifactu cancellation", null);
        }

        String emitterRazonSocial = command.getEmitterRazonSocial();
        if (!StringUtils.hasText(emitterRazonSocial)) {
            return rejectedCreditNote(command, "Business name (razon social) is required for Verifactu", null);
        }

        try {
            String previousHash = hashChainService.resolvePreviousHash(organizationId);
            VerifactuInvoiceAuthorization authorization = aeatClient.submitAnulacion(
                    command,
                    credentials,
                    nif,
                    emitterRazonSocial,
                    previousHash
            );

            if (!authorization.isAuthorized()) {
                return rejectedCreditNote(command, authorization.getRejectionReason(), authorization.getRequestXml());
            }

            return toAuthorizedResult(authorization, "anulacion");
        } catch (Exception ex) {
            log.error("Verifactu native cancellation failed: {}", ex.getMessage());
            return rejectedCreditNote(command, "Verifactu cancellation failed: " + ex.getMessage(), null);
        }
    }

    private String resolveNif(String emitterTaxId, VerifactuCredentials credentials) {
        if (StringUtils.hasText(emitterTaxId)) {
            return SpainTaxIdValidator.normalize(emitterTaxId);
        }
        if (credentials != null && StringUtils.hasText(credentials.nif())) {
            return SpainTaxIdValidator.normalize(credentials.nif());
        }
        String globalNif = verifactuProperties.getNif();
        return StringUtils.hasText(globalNif) ? SpainTaxIdValidator.normalize(globalNif) : null;
    }

    private EmitInvoiceResult toAuthorizedResult(VerifactuInvoiceAuthorization authorization, String operation) {
        return EmitInvoiceResult.builder()
                .tipoComprobante(authorization.getTipoComprobante())
                .puntoVenta(authorization.getPuntoVenta())
                .numeroComprobante(authorization.getNumeroComprobante())
                .cae(authorization.getHuella())
                .caeVencimiento(null)
                .pdfUrl(authorization.getQrUrl())
                .rawJson(toRawJson(authorization, operation))
                .authorized(true)
                .build();
    }

    private EmitInvoiceResult rejectedInvoice(EmitInvoiceCommand command, String reason, String rawXml) {
        return EmitInvoiceResult.builder()
                .tipoComprobante(command.getTipoComprobante())
                .puntoVenta(command.getPuntoVenta())
                .numeroComprobante(command.getNumeroComprobante())
                .authorized(false)
                .rejectionReason(reason)
                .rawJson(rawXml != null ? rawXml : toRejectedJson(reason))
                .build();
    }

    private EmitInvoiceResult rejectedCreditNote(EmitCreditNoteCommand command, String reason, String rawXml) {
        return EmitInvoiceResult.builder()
                .tipoComprobante(command.getTipoComprobante())
                .puntoVenta(command.getPuntoVenta())
                .numeroComprobante(command.getNumeroComprobante() != null
                        ? command.getNumeroComprobante()
                        : command.getRelatedInvoiceNumero())
                .authorized(false)
                .rejectionReason(reason)
                .rawJson(rawXml != null ? rawXml : toRejectedJson(reason))
                .build();
    }

    private String toRawJson(VerifactuInvoiceAuthorization authorization, String operation) {
        Map<String, Object> raw = new LinkedHashMap<>();
        raw.put("provider", "VERIFACTU_NATIVE");
        raw.put("operation", operation);
        raw.put("huella", authorization.getHuella());
        raw.put("qrUrl", authorization.getQrUrl());
        raw.put("punto_venta", authorization.getPuntoVenta());
        raw.put("numero_comprobante", authorization.getNumeroComprobante());
        raw.put("tipo_comprobante", authorization.getTipoComprobante() != null
                ? authorization.getTipoComprobante().name()
                : null);
        if (StringUtils.hasText(authorization.getResponseXml())) {
            raw.put("aeatResponse", authorization.getResponseXml());
        }
        return toJson(raw);
    }

    private String toRejectedJson(String reason) {
        Map<String, Object> raw = new LinkedHashMap<>();
        raw.put("provider", "VERIFACTU_NATIVE");
        raw.put("rejectionReason", reason);
        return toJson(raw);
    }

    private String toJson(Map<String, Object> raw) {
        try {
            return objectMapper.writeValueAsString(raw);
        } catch (JsonProcessingException ex) {
            return "{\"provider\":\"VERIFACTU_NATIVE\"}";
        }
    }
}
