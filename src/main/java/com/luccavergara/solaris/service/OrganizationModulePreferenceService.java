package com.luccavergara.solaris.service;

import com.luccavergara.solaris.dto.OrganizationModuleOptionResponse;
import com.luccavergara.solaris.dto.OrganizationModulePreferencesResponse;
import com.luccavergara.solaris.dto.UpdateOrganizationModulePreferencesRequest;
import com.luccavergara.solaris.entity.*;
import com.luccavergara.solaris.exception.ResourceNotFoundException;
import com.luccavergara.solaris.repository.OrganizationModuleAddonRepository;
import com.luccavergara.solaris.repository.OrganizationRepository;
import com.luccavergara.solaris.repository.PlanModuleGrantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class OrganizationModulePreferenceService {

    private static final String SELF_SERVICE_REFERENCE = "self_service";

    private final OrganizationRepository organizationRepository;
    private final SubscriptionService subscriptionService;
    private final PlanModuleGrantRepository planModuleGrantRepository;
    private final OrganizationModuleAddonRepository organizationModuleAddonRepository;

    @Transactional(readOnly = true)
    public OrganizationModulePreferencesResponse getPreferences(Long organizationId) {
        PreferenceContext context = loadPreferenceContext(organizationId);

        List<OrganizationModuleOptionResponse> modules = new ArrayList<>();

        for (ModuleCode moduleCode : context.autoActiveModules()) {
            if (moduleCode == ModuleCode.CORE) {
                continue;
            }

            modules.add(OrganizationModuleOptionResponse.builder()
                    .code(moduleCode)
                    .displayName(moduleCode.getDisplayName())
                    .includedInPlan(true)
                    .requiresOptIn(false)
                    .enabled(true)
                    .build());
        }

        Set<ModuleCode> enabledOptionalModules = context.enabledOptionalModules();

        for (ModuleCode moduleCode : context.optionalPlanModules()) {
            modules.add(OrganizationModuleOptionResponse.builder()
                    .code(moduleCode)
                    .displayName(moduleCode.getDisplayName())
                    .includedInPlan(true)
                    .requiresOptIn(true)
                    .enabled(enabledOptionalModules.contains(moduleCode))
                    .build());
        }

        modules.sort(Comparator.comparing(option -> option.getCode().name()));

        return OrganizationModulePreferencesResponse.builder()
                .modules(modules)
                .build();
    }

    @Transactional
    public OrganizationModulePreferencesResponse updatePreferences(
            Long organizationId,
            UpdateOrganizationModulePreferencesRequest request
    ) {
        PreferenceContext context = loadPreferenceContext(organizationId);
        Set<ModuleCode> requestedModules = new LinkedHashSet<>(request.getEnabledModules());
        requestedModules.retainAll(context.optionalPlanModules());

        LocalDateTime now = LocalDateTime.now();

        for (ModuleCode moduleCode : context.optionalPlanModules()) {
            boolean shouldEnable = requestedModules.contains(moduleCode);

            organizationModuleAddonRepository
                    .findByOrganizationIdAndModuleCodeAndSourceType(
                            organizationId,
                            moduleCode,
                            ModuleAddonSourceType.ORGANIZATION
                    )
                    .ifPresentOrElse(
                            existing -> updateExistingPreference(existing, shouldEnable, now),
                            () -> {
                                if (shouldEnable) {
                                    createPreference(context.organization(), moduleCode, now);
                                }
                            }
                    );
        }

        return getPreferences(organizationId);
    }

    private void updateExistingPreference(
            OrganizationModuleAddon existing,
            boolean shouldEnable,
            LocalDateTime now
    ) {
        if (shouldEnable) {
            existing.setStatus(ModuleAddonStatus.ACTIVE);
            existing.setRevokedAt(null);
            existing.setValidFrom(now);
            existing.setValidUntil(null);
        } else {
            existing.setStatus(ModuleAddonStatus.REVOKED);
            existing.setRevokedAt(now);
        }

        organizationModuleAddonRepository.save(existing);
    }

    private void createPreference(Organization organization, ModuleCode moduleCode, LocalDateTime now) {
        organizationModuleAddonRepository.save(
                OrganizationModuleAddon.builder()
                        .organization(organization)
                        .moduleCode(moduleCode)
                        .sourceType(ModuleAddonSourceType.ORGANIZATION)
                        .sourceReference(SELF_SERVICE_REFERENCE)
                        .status(ModuleAddonStatus.ACTIVE)
                        .validFrom(now)
                        .createdAt(now)
                        .build()
        );
    }

    private PreferenceContext loadPreferenceContext(Long organizationId) {
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));

        OrganizationSubscription subscription = subscriptionService.ensureSubscription(organization);

        if (!subscription.isBillingActive()) {
            throw new IllegalStateException("An active subscription is required to manage modules");
        }

        SubscriptionPlanCode effectivePlan = subscription.getPlanCode();
        Set<ModuleCode> planModules = new LinkedHashSet<>(
                planModuleGrantRepository.findModuleCodesByPlanCode(effectivePlan.name())
        );
        planModules.add(ModuleCode.CORE);

        Set<ModuleCode> optionalPlanModules = PlanModulePolicy.resolveOptionalPlanModules(
                effectivePlan,
                planModules
        );

        Set<ModuleCode> autoActiveModules = new LinkedHashSet<>(planModules);
        autoActiveModules.removeAll(optionalPlanModules);

        Set<ModuleCode> enabledOptionalModules = new LinkedHashSet<>(
                organizationModuleAddonRepository.findActiveModuleCodesByOrganizationIdAndSourceType(
                        organizationId,
                        ModuleAddonSourceType.ORGANIZATION,
                        ModuleAddonStatus.ACTIVE,
                        LocalDateTime.now()
                )
        );
        enabledOptionalModules.retainAll(optionalPlanModules);

        return new PreferenceContext(
                organization,
                optionalPlanModules,
                autoActiveModules,
                enabledOptionalModules
        );
    }

    private record PreferenceContext(
            Organization organization,
            Set<ModuleCode> optionalPlanModules,
            Set<ModuleCode> autoActiveModules,
            Set<ModuleCode> enabledOptionalModules
    ) {
    }
}
