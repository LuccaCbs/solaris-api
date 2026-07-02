package com.luccavergara.solaris.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TipoComprobante {
    FACTURA_B(6),
    FACTURA_C(11);

    private final int afipCode;
}
