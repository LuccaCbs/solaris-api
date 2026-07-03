package com.luccavergara.solaris.entity;

public enum PromoCodeType {

    /** Extends or grants a subscription plan (e.g. 30 days of BUSINESS). */
    GRANT_PLAN,

    /** Enables a single module add-on for a period. */
    GRANT_MODULE,

    /** Extends the current plan access window without changing plan tier. */
    EXTEND_ACCESS
}
