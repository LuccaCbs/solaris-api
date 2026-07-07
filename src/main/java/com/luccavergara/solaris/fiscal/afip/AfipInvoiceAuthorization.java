package com.luccavergara.solaris.fiscal.afip;

import com.luccavergara.solaris.entity.TipoComprobante;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class AfipInvoiceAuthorization {

    private final TipoComprobante tipoComprobante;
    private final Integer puntoVenta;
    private final Long numeroComprobante;
    private final String cae;
    private final LocalDate caeVencimiento;
    private final String rawXml;
    private final boolean authorized;
    private final String rejectionReason;

    public static AfipInvoiceAuthorization authorized(
            TipoComprobante tipoComprobante,
            Integer puntoVenta,
            Long numeroComprobante,
            String cae,
            LocalDate caeVencimiento,
            String rawXml
    ) {
        return AfipInvoiceAuthorization.builder()
                .tipoComprobante(tipoComprobante)
                .puntoVenta(puntoVenta)
                .numeroComprobante(numeroComprobante)
                .cae(cae)
                .caeVencimiento(caeVencimiento)
                .rawXml(rawXml)
                .authorized(true)
                .build();
    }

    public static AfipInvoiceAuthorization rejected(String rawXml, String rejectionReason) {
        return AfipInvoiceAuthorization.builder()
                .rawXml(rawXml)
                .authorized(false)
                .rejectionReason(rejectionReason)
                .build();
    }
}
