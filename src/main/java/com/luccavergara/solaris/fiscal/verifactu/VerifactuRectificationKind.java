package com.luccavergara.solaris.fiscal.verifactu;

/**
 * AEAT Verifactu rectificativa invoice type (list L2).
 */
public enum VerifactuRectificationKind {
    R1,
    R2,
    R3,
    R4,
    R5;

    public String code() {
        return name();
    }
}
