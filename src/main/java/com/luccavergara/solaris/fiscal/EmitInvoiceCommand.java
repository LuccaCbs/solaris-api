package com.luccavergara.solaris.fiscal;

import com.luccavergara.solaris.entity.TipoComprobante;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class EmitInvoiceCommand {

    private String emitterCuit;
    private String emitterRazonSocial;
    private Integer puntoVenta;
    private TipoComprobante tipoComprobante;
    private Long numeroComprobante;
    private BigDecimal importeNeto;
    private BigDecimal importeIva;
    private BigDecimal importeTotal;
    private String customerDocumentType;
    private String customerDocumentNumber;
    private String customerRazonSocial;
    private List<InvoiceLineCommand> lines;

    @Getter
    @Builder
    public static class InvoiceLineCommand {
        private String description;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal subtotal;
        private BigDecimal ivaRate;
        private BigDecimal netAmount;
        private BigDecimal ivaAmount;
    }
}
