package com.luccavergara.solaris.fiscal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.luccavergara.solaris.entity.FiscalDocumentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Component
@RequiredArgsConstructor
public class MockFiscalProvider implements FiscalProvider {

    private final ObjectMapper objectMapper;

    @Override
    public EmitInvoiceResult emitInvoice(EmitInvoiceCommand command) {
        String cae = generateFakeCae();
        LocalDate vencimiento = LocalDate.now().plusDays(10);

        Map<String, Object> raw = new LinkedHashMap<>();
        raw.put("provider", "MOCK");
        raw.put("cae", cae);
        raw.put("cae_vencimiento", vencimiento.toString());
        raw.put("punto_venta", command.getPuntoVenta());
        raw.put("numero_comprobante", command.getNumeroComprobante());
        raw.put("tipo_comprobante", command.getTipoComprobante().name());
        raw.put("importe_total", command.getImporteTotal());

        return EmitInvoiceResult.builder()
                .tipoComprobante(command.getTipoComprobante())
                .puntoVenta(command.getPuntoVenta())
                .numeroComprobante(command.getNumeroComprobante())
                .cae(cae)
                .caeVencimiento(vencimiento)
                .pdfUrl(null)
                .rawJson(toJson(raw))
                .authorized(true)
                .build();
    }

    @Override
    public EmitInvoiceResult emitCreditNote(EmitCreditNoteCommand command) {
        throw new UnsupportedOperationException("Credit notes are not supported yet");
    }

    private String generateFakeCae() {
        long value = ThreadLocalRandom.current().nextLong(1_000_000_000_000L, 9_999_999_999_999L);
        return String.valueOf(value);
    }

    private String toJson(Map<String, Object> raw) {
        try {
            return objectMapper.writeValueAsString(raw);
        } catch (JsonProcessingException ex) {
            return "{\"provider\":\"MOCK\",\"status\":\"" + FiscalDocumentStatus.AUTHORIZED.name() + "\"}";
        }
    }
}
