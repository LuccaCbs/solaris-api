package com.luccavergara.solaris.fiscal;

import com.luccavergara.solaris.entity.TipoComprobante;
import com.luccavergara.solaris.fiscal.verifactu.VerifactuCorrectionType;
import com.luccavergara.solaris.fiscal.verifactu.VerifactuRectificationKind;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

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
    /** When set with amounts, submits a rectificativa (R1–R5) instead of an anulación. */
    private VerifactuRectificationKind rectificationKind;
    /** S = sustitución, I = por diferencias (default for credit notes). */
    private VerifactuCorrectionType correctionType;
    private BigDecimal importeNeto;
    private BigDecimal importeIva;
    private BigDecimal importeTotal;
    /** Original invoice base (required for correction type S). */
    private BigDecimal correctedBaseAmount;
    /** Original invoice VAT (required for correction type S). */
    private BigDecimal correctedTaxAmount;
}
