package com.luccavergara.solaris.fiscal;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class TusFacturasFiscalProvider implements FiscalProvider {

    private final ObjectMapper objectMapper;

    @Value("${application.fiscal.tusfacturas.base-url:https://www.tusfacturas.app/app/api/v1/facturacion/nuevo}")
    private String baseUrl;

    @Override
    public EmitInvoiceResult emitInvoice(EmitInvoiceCommand command) {
        Map<String, Object> payload = buildPayload(command);

        try {
            RestClient restClient = RestClient.create();
            String responseBody = restClient.post()
                    .uri(baseUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(String.class);

            return parseResponse(command, responseBody);
        } catch (RestClientException ex) {
            log.error("TusFacturas API call failed: {}", ex.getMessage());
            return EmitInvoiceResult.builder()
                    .tipoComprobante(command.getTipoComprobante())
                    .puntoVenta(command.getPuntoVenta())
                    .numeroComprobante(command.getNumeroComprobante())
                    .authorized(false)
                    .rejectionReason(ex.getMessage())
                    .rawJson(toJson(Map.of("error", ex.getMessage())))
                    .build();
        }
    }

    @Override
    public EmitInvoiceResult emitCreditNote(EmitCreditNoteCommand command) {
        throw new UnsupportedOperationException("Credit notes are not supported yet");
    }

    private Map<String, Object> buildPayload(EmitInvoiceCommand command) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("punto_venta", command.getPuntoVenta());
        payload.put("tipo_comprobante", command.getTipoComprobante().getAfipCode());
        payload.put("numero", command.getNumeroComprobante());
        payload.put("importe_total", command.getImporteTotal());
        payload.put("importe_neto", command.getImporteNeto());
        payload.put("importe_iva", command.getImporteIva());
        payload.put("cliente", Map.of(
                "documento", command.getCustomerDocumentNumber(),
                "razon_social", command.getCustomerRazonSocial()
        ));
        return payload;
    }

    private EmitInvoiceResult parseResponse(EmitInvoiceCommand command, String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            boolean authorized = root.path("autorizado").asBoolean(false)
                    || root.path("cae").asText(null) != null;

            return EmitInvoiceResult.builder()
                    .tipoComprobante(command.getTipoComprobante())
                    .puntoVenta(command.getPuntoVenta())
                    .numeroComprobante(command.getNumeroComprobante())
                    .cae(root.path("cae").asText(null))
                    .caeVencimiento(parseDate(root.path("cae_vencimiento").asText(null)))
                    .pdfUrl(root.path("pdf_url").asText(null))
                    .rawJson(responseBody)
                    .authorized(authorized)
                    .rejectionReason(root.path("error").asText(null))
                    .build();
        } catch (Exception ex) {
            return EmitInvoiceResult.builder()
                    .tipoComprobante(command.getTipoComprobante())
                    .puntoVenta(command.getPuntoVenta())
                    .numeroComprobante(command.getNumeroComprobante())
                    .authorized(false)
                    .rejectionReason("Invalid TusFacturas response")
                    .rawJson(responseBody)
                    .build();
        }
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return LocalDate.parse(value);
    }

    private String toJson(Map<String, Object> raw) {
        try {
            return objectMapper.writeValueAsString(raw);
        } catch (Exception ex) {
            return "{}";
        }
    }
}
