package com.luccavergara.solaris.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BillingJurisdiction {

    AR(BillingProvider.MERCADOPAGO, "ARS"),
    EU(BillingProvider.STRIPE, "EUR");

    private final BillingProvider defaultProvider;
    private final String defaultCurrency;
}
