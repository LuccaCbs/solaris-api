package com.luccavergara.solaris.controller;

import com.luccavergara.solaris.dto.AccessContextResponse;
import com.luccavergara.solaris.entity.OrganizationMemberRole;
import com.luccavergara.solaris.entity.OrganizationMemberStatus;
import com.luccavergara.solaris.entity.User;
import com.luccavergara.solaris.repository.OrganizationMemberRepository;
import com.luccavergara.solaris.security.OrganizationSecurity;
import com.luccavergara.solaris.service.AuthenticatedUserService;
import com.luccavergara.solaris.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
public class MeController {

    private final AuthenticatedUserService authenticatedUserService;
    private final OrganizationMemberRepository organizationMemberRepository;
    private final OrganizationSecurity organizationSecurity;

    @GetMapping("/access-context")
    public AccessContextResponse getAccessContext(@RequestParam(required = false) Long orgId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean authenticated = authentication != null && authentication.isAuthenticated();

        User user = null;
        String email = null;

        if (authenticated) {
            try {
                user = authenticatedUserService.getCurrentUser();
                email = user.getEmail();
            } catch (RuntimeException ex) {
                email = authentication.getName();
            }
        }

        String membershipRole = null;
        String membershipStatus = null;

        if (user != null && orgId != null) {
            var membership = organizationMemberRepository.findByUserAndOrganizationId(user, orgId);

            if (membership.isPresent()) {
                membershipRole = membership.get().getRole().name();
                membershipStatus = membership.get().getStatus().name();
            }
        }

        boolean canAccessAsAdmin = orgId != null
                && organizationSecurity.canAccessOrganization(orgId, OrganizationMemberRole.ADMIN);
        String denialReason = OrganizationSecurity.consumeLastDenialReason();

        return AccessContextResponse.builder()
                .email(email)
                .authenticated(authenticated)
                .jwtOrgId(TenantContext.getOrganizationId())
                .jwtRole(formatRole(TenantContext.getRole()))
                .tenantOrgId(TenantContext.getOrganizationId())
                .tenantRole(formatRole(TenantContext.getRole()))
                .requestedOrgId(orgId)
                .membershipRole(membershipRole)
                .membershipStatus(membershipStatus)
                .canAccessAsAdmin(canAccessAsAdmin)
                .denialReason(denialReason)
                .build();
    }

    private String formatRole(OrganizationMemberRole role) {
        return role != null ? role.name() : null;
    }
}
