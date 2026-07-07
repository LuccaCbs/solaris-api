package com.luccavergara.solaris.fiscal.afip;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.luccavergara.solaris.entity.TipoComprobante;
import com.luccavergara.solaris.fiscal.EmitCreditNoteCommand;
import com.luccavergara.solaris.fiscal.EmitInvoiceCommand;
import com.luccavergara.solaris.fiscal.EmitInvoiceResult;
import com.luccavergara.solaris.fiscal.FiscalProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AfipNativeFiscalProvider implements FiscalProvider {

    private final AfipProperties afipProperties;
    private final AfipWsfeClient wsfeClient;
    private final ObjectMapper objectMapper;

    @Override
    public EmitInvoiceResult emitInvoice(EmitInvoiceCommand command) {
        throw new IllegalStateException(
                "AFIP native provider requires credentials; use emitInvoice(command, credentials)"
        );
    }

    public EmitInvoiceResult emitInvoice(EmitInvoiceCommand command, AfipCredentials credentials) {
        if (!afipProperties.getHomologation().isEnabled()) {
            return rejected(
                    command,
                    "AFIP homologation is disabled (set afip.homologation.enabled=true)",
                    null
            );
        }

        if (command.getTipoComprobante() != TipoComprobante.FACTURA_C) {
            return rejected(
                    command,
                    "AFIP native POC supports Factura C only — use TusFacturas or MOCK for other types",
                    null
            );
        }

        String cuit = resolveCuit(command, credentials);
        if (!StringUtils.hasText(cuit)) {
            return rejected(command, "AFIP CUIT is required (organization or afip.cuit)", null);
        }

        try {
            AfipInvoiceAuthorization authorization = wsfeClient.authorizeInvoice(command, credentials, cuit);
            if (!authorization.isAuthorized()) {
                return rejected(command, authorization.getRejectionReason(), authorization.getRawXml());
            }

            return EmitInvoiceResult.builder()
                    .tipoComprobante(authorization.getTipoComprobante())
                    .puntoVenta(authorization.getPuntoVenta())
                    .numeroComprobante(authorization.getNumeroComprobante())
                    .cae(authorization.getCae())
                    .caeVencimiento(authorization.getCaeVencimiento())
                    .pdfUrl(null)
                    .rawJson(toRawJson(authorization))
                    .authorized(true)
                    .build();
        } catch (Exception ex) {
            log.error("AFIP native invoice emission failed: {}", ex.getMessage());
            return rejected(command, "AFIP request failed: " + ex.getMessage(), null);
        }
    }

    @Override
    public EmitInvoiceResult emitCreditNote(EmitCreditNoteCommand command) {
        throw new UnsupportedOperationException(
                "AFIP native credit notes are not supported in POC — planned for Phase 1"
        );
    }

    private String resolveCuit(EmitInvoiceCommand command, AfipCredentials credentials) {
        if (StringUtils.hasText(command.getEmitterCuit())) {
            return command.getEmitterCuit().replaceAll("\\D", "");
        }
        if (credentials != null && StringUtils.hasText(credentials.cuit())) {
            return credentials.cuit().replaceAll("\\D", "");
        }
        String globalCuit = afipProperties.getCuit();
        return StringUtils.hasText(globalCuit) ? globalCuit.replaceAll("\\D", "") : null;
    }

    private EmitInvoiceResult rejected(EmitInvoiceCommand command, String reason, String rawXml) {
        return EmitInvoiceResult.builder()
                .tipoComprobante(command.getTipoComprobante())
                .puntoVenta(command.getPuntoVenta())
                .numeroComprobante(command.getNumeroComprobante())
                .authorized(false)
                .rejectionReason(reason)
                .rawJson(rawXml != null ? rawXml : toRejectedJson(reason))
                .build();
    }

    private String toRawJson(AfipInvoiceAuthorization authorization) {
        Map<String, Object> raw = new LinkedHashMap<>();
        raw.put("provider", "AFIP_NATIVE");
        raw.put("cae", authorization.getCae());
        raw.put("cae_vencimiento", authorization.getCaeVencimiento());
        raw.put("punto_venta", authorization.getPuntoVenta());
        raw.put("numero_comprobante", authorization.getNumeroComprobante());
        raw.put("tipo_comprobante", authorization.getTipoComprobante() != null
                ? authorization.getTipoComprobante().name()
                : null);
        return toJson(raw);
    }

    private String toRejectedJson(String reason) {
        Map<String, Object> raw = new LinkedHashMap<>();
        raw.put("provider", "AFIP_NATIVE");
        raw.put("rejectionReason", reason);
        return toJson(raw);
    }

    private String toJson(Map<String, Object> raw) {
        try {
            return objectMapper.writeValueAsString(raw);
        } catch (JsonProcessingException ex) {
            return "{\"provider\":\"AFIP_NATIVE\"}";
        }
    }
}
