package com.luccavergara.solaris.service;

import com.luccavergara.solaris.entity.ModuleCode;
import com.luccavergara.solaris.entity.SubscriptionPlanCode;

import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Set;

final class PlanModulePolicy {

    private static final Set<ModuleCode> BUSINESS_OPTIONAL_MODULES = EnumSet.of(
            ModuleCode.CUSTOMERS,
            ModuleCode.FISCAL,
            ModuleCode.TEAM,
            ModuleCode.AUDIT,
            ModuleCode.ANALYTICS
    );

    private PlanModulePolicy() {
    }

    static Set<ModuleCode> resolveOptionalPlanModules(
            SubscriptionPlanCode planCode,
            Set<ModuleCode> planModules
    ) {
        if (planCode == SubscriptionPlanCode.POS) {
            return Set.of();
        }

        Set<ModuleCode> optionalModules = new LinkedHashSet<>(BUSINESS_OPTIONAL_MODULES);
        optionalModules.retainAll(planModules);
        return optionalModules;
    }
}
