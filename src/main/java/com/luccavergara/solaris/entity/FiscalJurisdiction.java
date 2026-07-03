package com.luccavergara.solaris.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum FiscalJurisdiction {

    AR_AFIP(FiscalProviderType.TUSFACTURAS),
    ES_VERIFACTU(FiscalProviderType.MOCK);

    private final FiscalProviderType defaultProviderType;
}
