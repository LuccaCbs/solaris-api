package com.luccavergara.solaris.fiscal;

import com.luccavergara.solaris.entity.TipoComprobante;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class EmitCreditNoteCommand {

    private String emitterCuit;
    private Integer puntoVenta;
    private TipoComprobante tipoComprobante;
    private Long relatedInvoiceNumero;
}
