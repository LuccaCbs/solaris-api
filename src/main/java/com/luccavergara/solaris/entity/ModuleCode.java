package com.luccavergara.solaris.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Functional modules that can be included in a plan or granted as add-ons / promo codes.
 * CORE is always required for any organization to operate.
 */
@Getter
@RequiredArgsConstructor
public enum ModuleCode {

    CORE("Core POS", true),
    INVENTORY("Inventory", false),
    CUSTOMERS("Customers", false),
    FISCAL("E-Invoicing", false),
    TEAM("Team", false),
    MULTI_STORE("Multi-store", false),
    AUDIT("Audit", false),
    ANALYTICS("Analytics", false);

    private final String displayName;
    private final boolean alwaysOn;

    public boolean isAlwaysOn() {
        return alwaysOn;
    }
}
