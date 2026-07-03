package com.luccavergara.solaris.service;

import com.luccavergara.solaris.dto.OrganizationResponse;
import com.luccavergara.solaris.dto.UpdateOrganizationMemberRoleRequest;
import com.luccavergara.solaris.dto.UpdateOrganizationRequest;
import com.luccavergara.solaris.entity.*;
import com.luccavergara.solaris.exception.ResourceNotFoundException;
import com.luccavergara.solaris.repository.OrganizationMemberRepository;
import com.luccavergara.solaris.repository.OrganizationRepository;
import com.luccavergara.solaris.repository.StoreRepository;
import com.luccavergara.solaris.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository organizationMemberRepository;
    private final StoreRepository storeRepository;
    private final AuthenticatedUserService authenticatedUserService;
    private final OrganizationMembershipService organizationMembershipService;

    @Transactional(readOnly = true)
    public OrganizationResponse getOrganization(Long organizationId) {
        validateOrganizationAccess(organizationId);

        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));

        return mapToResponse(organization);
    }

    @Transactional
    public OrganizationResponse updateOrganization(Long organizationId, UpdateOrganizationRequest request) {
        validateOrganizationAccess(organizationId);

        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));

        organization.setDisplayName(request.getDisplayName().trim());
        Organization saved = organizationRepository.save(organization);

        return mapToResponse(saved);
    }

    @Transactional
    public void updateMemberRole(
            Long organizationId,
            Long memberId,
            UpdateOrganizationMemberRoleRequest request
    ) {
        validateOrganizationAccess(organizationId);

        OrganizationMemberRole actorRole = TenantContext.getRole();

        if (actorRole == null || actorRole.getPrivilegeLevel() < OrganizationMemberRole.ADMIN.getPrivilegeLevel()) {
            throw new IllegalArgumentException("You do not have permission to update member roles");
        }

        OrganizationMember member = organizationMemberRepository
                .findByIdAndOrganizationId(memberId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));

        if (member.getStatus() != OrganizationMemberStatus.ACTIVE) {
            throw new IllegalArgumentException("Only active members can be updated");
        }

        User currentUser = authenticatedUserService.getCurrentUser();

        if (member.getUser().getId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("You cannot change your own role");
        }

        if (request.getRole() == OrganizationMemberRole.OWNER) {
            throw new IllegalArgumentException("Cannot assign OWNER role");
        }

        if (member.getRole() == OrganizationMemberRole.OWNER) {
            throw new IllegalArgumentException("Cannot change the organization owner's role");
        }

        if (actorRole == OrganizationMemberRole.ADMIN) {
            if (member.getRole() == OrganizationMemberRole.ADMIN || request.getRole() == OrganizationMemberRole.ADMIN) {
                throw new IllegalArgumentException("Only the owner can manage admin roles");
            }
        }

        Store store = resolveStore(organizationId, request.getStoreId());
        member.setRole(request.getRole());
        member.setStore(store);
        organizationMemberRepository.save(member);
    }

    private Store resolveStore(Long organizationId, Long storeId) {
        if (storeId == null) {
            return null;
        }

        organizationMembershipService.validateStoreInOrganization(organizationId, storeId);

        return storeRepository.findById(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Store not found"));
    }

    private OrganizationResponse mapToResponse(Organization organization) {
        String displayName = organization.getDisplayName();

        if (displayName == null || displayName.isBlank()) {
            displayName = organization.getRazonSocial();
        }

        return OrganizationResponse.builder()
                .id(organization.getId())
                .displayName(displayName)
                .razonSocial(organization.getRazonSocial())
                .timezone(organization.getTimezone())
                .build();
    }

    private void validateOrganizationAccess(Long organizationId) {
        Long currentOrgId = TenantContext.getOrganizationId();

        if (currentOrgId == null || !currentOrgId.equals(organizationId)) {
            throw new ResourceNotFoundException("Organization not found");
        }
    }
}
