package com.luccavergara.solaris.service;

import com.luccavergara.solaris.entity.ModuleCode;
import com.luccavergara.solaris.entity.OrganizationMember;
import com.luccavergara.solaris.entity.OrganizationMemberRole;
import com.luccavergara.solaris.entity.OrganizationMemberStatus;
import com.luccavergara.solaris.entity.SubscriptionPlanCode;
import com.luccavergara.solaris.entity.User;
import com.luccavergara.solaris.exception.ResourceNotFoundException;
import com.luccavergara.solaris.repository.OrganizationMemberRepository;
import com.luccavergara.solaris.repository.PlanModuleGrantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrganizationSeedService {

    private final CategoryService categoryService;
    private final OrganizationMemberRepository organizationMemberRepository;
    private final PlanModuleGrantRepository planModuleGrantRepository;

    @Transactional
    public void seedForActivatedPlan(Long organizationId, SubscriptionPlanCode planCode) {
        if (!planIncludesInventory(planCode)) {
            return;
        }

        ensureDefaultCategory(organizationId);
    }

    @Transactional
    public void ensureDefaultCategory(Long organizationId) {
        User owner = resolveOwnerUser(organizationId);
        categoryService.ensureOrganizationDefaultCategory(organizationId, owner);
    }

    private boolean planIncludesInventory(SubscriptionPlanCode planCode) {
        return planModuleGrantRepository.findModuleCodesByPlanCode(planCode.name())
                .contains(ModuleCode.INVENTORY);
    }

    private User resolveOwnerUser(Long organizationId) {
        List<OrganizationMember> members = organizationMemberRepository.findAllByOrganizationId(organizationId);

        return members.stream()
                .filter(member -> member.getStatus() == OrganizationMemberStatus.ACTIVE)
                .filter(member -> member.getRole() == OrganizationMemberRole.OWNER)
                .map(OrganizationMember::getUser)
                .findFirst()
                .or(() -> members.stream()
                        .filter(member -> member.getStatus() == OrganizationMemberStatus.ACTIVE)
                        .map(OrganizationMember::getUser)
                        .findFirst())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No active organization member found to seed defaults"
                ));
    }
}
