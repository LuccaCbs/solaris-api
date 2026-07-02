package com.luccavergara.solaris.fiscal;

import com.luccavergara.solaris.entity.CondicionIva;
import com.luccavergara.solaris.entity.ProductIvaRate;
import com.luccavergara.solaris.entity.TipoComprobante;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class FiscalInvoiceCalculator {

    private static final int SCALE = 2;

    private FiscalInvoiceCalculator() {
    }

    public static TipoComprobante resolveTipoComprobante(CondicionIva emitterCondicionIva) {
        return switch (emitterCondicionIva) {
            case MONOTRIBUTO, EXENTO -> TipoComprobante.FACTURA_C;
            case RESPONSABLE_INSCRIPTO, CONSUMIDOR_FINAL, NO_CATEGORIZADO -> TipoComprobante.FACTURA_B;
        };
    }

    public static BigDecimal ivaRateFactor(ProductIvaRate ivaRate) {
        return switch (ivaRate) {
            case EXENTO -> BigDecimal.ZERO;
            case REDUCIDO_10_5 -> new BigDecimal("0.105");
            case GENERAL_21 -> new BigDecimal("0.21");
        };
    }

    public static InvoiceAmounts calculateFromGrossLine(
            BigDecimal grossSubtotal,
            ProductIvaRate ivaRate,
            boolean includesIvaBreakdown
    ) {
        if (!includesIvaBreakdown || grossSubtotal == null || grossSubtotal.signum() == 0) {
            return new InvoiceAmounts(
                    grossSubtotal != null ? grossSubtotal : BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    grossSubtotal != null ? grossSubtotal : BigDecimal.ZERO
            );
        }

        BigDecimal rate = ivaRateFactor(ivaRate);
        if (rate.signum() == 0) {
            return new InvoiceAmounts(grossSubtotal, BigDecimal.ZERO, grossSubtotal);
        }

        BigDecimal divisor = BigDecimal.ONE.add(rate);
        BigDecimal net = grossSubtotal.divide(divisor, SCALE, RoundingMode.HALF_UP);
        BigDecimal iva = grossSubtotal.subtract(net).setScale(SCALE, RoundingMode.HALF_UP);

        return new InvoiceAmounts(net, iva, grossSubtotal);
    }

    public record InvoiceAmounts(
            BigDecimal neto,
            BigDecimal iva,
            BigDecimal total
    ) {
        public InvoiceAmounts add(InvoiceAmounts other) {
            return new InvoiceAmounts(
                    neto.add(other.neto),
                    iva.add(other.iva),
                    total.add(other.total)
            );
        }
    }
}
