package com.luccavergara.solaris.fiscal.verifactu;

/**
 * AEAT Verifactu rectificativa correction method (list L3).
 */
public enum VerifactuCorrectionType {
    /** Rectificación por sustitución. */
    S,
    /** Rectificación por diferencias (notas de crédito/débito). */
    I;

    public String code() {
        return name();
    }
}
