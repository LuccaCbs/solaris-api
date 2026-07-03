package com.luccavergara.solaris.billing;

import java.math.BigDecimal;

public record BillingPrice(
        BigDecimal unitAmount,
        String currency
) {
    public boolean isArs() {
        return "ARS".equalsIgnoreCase(currency);
    }
}
