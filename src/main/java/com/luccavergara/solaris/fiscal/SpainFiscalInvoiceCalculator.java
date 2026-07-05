package com.luccavergara.solaris.fiscal;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class SpainFiscalInvoiceCalculator {

    private static final BigDecimal IVA_RATE = new BigDecimal("0.21");

    private SpainFiscalInvoiceCalculator() {
    }

    public static InvoiceTotals calculateFromTotal(BigDecimal importeTotal) {
        BigDecimal total = importeTotal.setScale(2, RoundingMode.HALF_UP);
        BigDecimal neto = total.divide(BigDecimal.ONE.add(IVA_RATE), 2, RoundingMode.HALF_UP);
        BigDecimal iva = total.subtract(neto).setScale(2, RoundingMode.HALF_UP);

        return new InvoiceTotals(neto, iva, total);
    }

    public record InvoiceTotals(BigDecimal neto, BigDecimal iva, BigDecimal total) {
    }
}
