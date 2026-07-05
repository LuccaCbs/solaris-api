package com.luccavergara.solaris.entity;

public enum ModuleAddonSourceType {

    /** Purchased through Mercado Pago or mock billing. */
    BILLING,

    /** Granted by a promo / gift code redemption. */
    PROMO_CODE,

    /** Granted manually by a platform operator. */
    OPERATOR,

    /** Legacy migration or internal adjustment. */
    SYSTEM,

    /** Enabled by the organization owner through self-service preferences. */
    ORGANIZATION
}
