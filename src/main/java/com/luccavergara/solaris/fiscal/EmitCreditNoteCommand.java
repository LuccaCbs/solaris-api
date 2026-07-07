package com.luccavergara.solaris.fiscal;

import com.luccavergara.solaris.entity.TipoComprobante;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class EmitCreditNoteCommand {

    private String emitterCuit;
    private String emitterRazonSocial;
    private Integer puntoVenta;
    private TipoComprobante tipoComprobante;
    private Long numeroComprobante;
    private Long relatedInvoiceNumero;
    /** NumSerieFactura of the invoice being cancelled (e.g. {@code 1-42}). */
    private String relatedNumSerieFactura;
    /** Expedition date of the cancelled invoice in {@code dd-MM-yyyy} format. */
    private String relatedFechaExpedicion;
}
