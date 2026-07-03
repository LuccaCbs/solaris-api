package com.luccavergara.solaris.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SubscriptionPlanCode {

    /** Freemium public plan for new organizations. */
    POS(false, true),

    /** Full cloud + e-invoicing bundle. Grandfather target for early testers. */
    BUSINESS(false, true),

    /** Multi-store premium bundle. */
    SCALE(false, true),

    /** Operator-only plan for testers and partners. Never shown in public billing. */
    INTERNAL(true, false);

    private final boolean operatorOnly;
    private final boolean publiclyListed;
}
