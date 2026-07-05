package com.luccavergara.solaris.service;

import com.luccavergara.solaris.dto.OrganizationEntitlementsResponse;
import com.luccavergara.solaris.entity.*;
import com.luccavergara.solaris.exception.ModuleAccessException;
import com.luccavergara.solaris.exception.ResourceNotFoundException;
import com.luccavergara.solaris.repository.OrganizationModuleAddonRepository;
import com.luccavergara.solaris.repository.OrganizationRepository;
import com.luccavergara.solaris.repository.PlanModuleGrantRepository;
import com.luccavergara.solaris.repository.PromoCodeRedemptionRepository;
import com.luccavergara.solaris.repository.SubscriptionPlanRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class EntitlementService {

    private static final SubscriptionPlanCode FALLBACK_PLAN = SubscriptionPlanCode.POS;

    private final OrganizationRepository organizationRepository;
    private final SubscriptionService subscriptionService;
    private final PlanModuleGrantRepository planModuleGrantRepository;
    private final OrganizationModuleAddonRepository organizationModuleAddonRepository;
    private final PromoCodeRedemptionRepository promoCodeRedemptionRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;

    public EntitlementService(
            OrganizationRepository organizationRepository,
            @Lazy SubscriptionService subscriptionService,
            PlanModuleGrantRepository planModuleGrantRepository,
            OrganizationModuleAddonRepository organizationModuleAddonRepository,
            PromoCodeRedemptionRepository promoCodeRedemptionRepository,
            SubscriptionPlanRepository subscriptionPlanRepository
    ) {
        this.organizationRepository = organizationRepository;
        this.subscriptionService = subscriptionService;
        this.planModuleGrantRepository = planModuleGrantRepository;
        this.organizationModuleAddonRepository = organizationModuleAddonRepository;
        this.promoCodeRedemptionRepository = promoCodeRedemptionRepository;
        this.subscriptionPlanRepository = subscriptionPlanRepository;
    }

    @Transactional(readOnly = true)
    public OrganizationEntitlementsResponse getEntitlements(Long organizationId) {
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));

        OrganizationSubscription subscription = subscriptionService.ensureSubscription(organization);
        return buildEntitlements(organizationId, subscription);
    }

    @Transactional(readOnly = true)
    public Set<ModuleCode> getActiveModules(Long organizationId) {
        return new LinkedHashSet<>(getEntitlements(organizationId).getActiveModules());
    }

    @Transactional(readOnly = true)
    public boolean hasModule(Long organizationId, ModuleCode moduleCode) {
        if (moduleCode.isAlwaysOn()) {
            return true;
        }

        return getActiveModules(organizationId).contains(moduleCode);
    }

    @Transactional(readOnly = true)
    public void assertModule(Long organizationId, ModuleCode moduleCode) {
        if (hasModule(organizationId, moduleCode)) {
            return;
        }

        throw new ModuleAccessException(
                moduleCode,
                "Module " + moduleCode.name() + " is not enabled for this organization"
        );
    }

    private OrganizationEntitlementsResponse buildEntitlements(
            Long organizationId,
            OrganizationSubscription subscription
    ) {
        if (subscription.getStatus() == SubscriptionStatus.PENDING_PLAN) {
            return OrganizationEntitlementsResponse.builder()
                    .planModules(toSortedList(EnumSet.of(ModuleCode.CORE)))
                    .addonModules(List.of())
                    .promoModules(List.of())
                    .activeModules(toSortedList(EnumSet.of(ModuleCode.CORE)))
                    .build();
        }

        LocalDateTime now = LocalDateTime.now();
        SubscriptionPlanCode effectivePlan = resolveEffectivePlanCode(subscription);

        Set<ModuleCode> planModules = new LinkedHashSet<>(
                planModuleGrantRepository.findModuleCodesByPlanCode(effectivePlan.name())
        );
        planModules.add(ModuleCode.CORE);

        Set<ModuleCode> addonModules = new LinkedHashSet<>(
                organizationModuleAddonRepository.findActiveModuleCodesByOrganizationId(
                        organizationId,
                        ModuleAddonStatus.ACTIVE,
                        now
                )
        );

        Set<ModuleCode> promoModules = resolvePromoModules(organizationId, now);

        Set<ModuleCode> activeModules = EnumSet.noneOf(ModuleCode.class);
        activeModules.addAll(planModules);
        activeModules.addAll(addonModules);
        activeModules.addAll(promoModules);
        activeModules.add(ModuleCode.CORE);

        return OrganizationEntitlementsResponse.builder()
                .planModules(toSortedList(planModules))
                .addonModules(toSortedList(addonModules))
                .promoModules(toSortedList(promoModules))
                .activeModules(toSortedList(activeModules))
                .build();
    }

    private SubscriptionPlanCode resolveEffectivePlanCode(OrganizationSubscription subscription) {
        if (!subscription.isBillingActive()) {
            return FALLBACK_PLAN;
        }

        return subscription.getPlanCode();
    }

    private Set<ModuleCode> resolvePromoModules(Long organizationId, LocalDateTime now) {
        Set<ModuleCode> promoModules = new LinkedHashSet<>();

        List<PromoCodeRedemption> activeRedemptions = promoCodeRedemptionRepository
                .findActiveRedemptionsByOrganizationId(
                        organizationId,
                        PromoRedemptionStatus.ACTIVE,
                        now
                );

        for (PromoCodeRedemption redemption : activeRedemptions) {
            if (redemption.getGrantedModuleCode() != null) {
                promoModules.add(redemption.getGrantedModuleCode());
            }

            if (redemption.getGrantedPlanCode() != null) {
                promoModules.addAll(
                        planModuleGrantRepository.findModuleCodesByPlanCode(
                                redemption.getGrantedPlanCode().name()
                        )
                );
            }
        }

        return promoModules;
    }

    @Transactional(readOnly = true)
    public String resolvePlanDisplayName(SubscriptionPlanCode planCode) {
        return subscriptionPlanRepository.findById(planCode)
                .map(SubscriptionPlan::getDisplayName)
                .orElse(planCode.name());
    }

    private List<ModuleCode> toSortedList(Set<ModuleCode> modules) {
        List<ModuleCode> sorted = new ArrayList<>(modules);
        sorted.sort(Comparator.comparing(Enum::name));
        return sorted;
    }
}
