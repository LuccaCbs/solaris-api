package com.luccavergara.solaris.fiscal;

import com.luccavergara.solaris.entity.TipoComprobante;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class EmitInvoiceResult {

    private TipoComprobante tipoComprobante;
    private Integer puntoVenta;
    private Long numeroComprobante;
    private String cae;
    private LocalDate caeVencimiento;
    private String pdfUrl;
    private String rawJson;
    private boolean authorized;
    private String rejectionReason;
}
