package com.luccavergara.solaris.security;

import com.luccavergara.solaris.entity.OrganizationMember;
import com.luccavergara.solaris.entity.OrganizationMemberRole;
import com.luccavergara.solaris.entity.OrganizationMemberStatus;
import com.luccavergara.solaris.entity.User;
import com.luccavergara.solaris.repository.OrganizationMemberRepository;
import com.luccavergara.solaris.service.AuthenticatedUserService;
import com.luccavergara.solaris.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component("organizationSecurity")
@RequiredArgsConstructor
public class OrganizationSecurity {

    private final AuthenticatedUserService authenticatedUserService;
    private final OrganizationMemberRepository organizationMemberRepository;

    public boolean hasMinimumRole(OrganizationMemberRole minimumRole) {
        OrganizationMemberRole currentRole = resolveCurrentRole();

        if (currentRole == null) {
            return false;
        }

        return currentRole.getPrivilegeLevel() >= minimumRole.getPrivilegeLevel();
    }

    public boolean belongsToOrganization(Long organizationId) {
        if (organizationId == null) {
            return false;
        }

        Long currentOrgId = TenantContext.getOrganizationId();
        return currentOrgId != null && currentOrgId.equals(organizationId);
    }

    public boolean canAccessOrganization(Long organizationId, OrganizationMemberRole minimumRole) {
        if (organizationId == null) {
            return false;
        }

        Optional<OrganizationMemberRole> membershipRole = resolveMembershipRole(organizationId);

        if (membershipRole.isPresent()) {
            boolean allowed = membershipRole.get().getPrivilegeLevel() >= minimumRole.getPrivilegeLevel();

            if (!allowed) {
                log.debug(
                        "Organization access denied for orgId={}: role {} below minimum {}",
                        organizationId,
                        membershipRole.get(),
                        minimumRole
                );
            }

            return allowed;
        }

        boolean allowed = belongsToOrganization(organizationId) && hasMinimumRole(minimumRole);

        if (!allowed) {
            log.debug(
                    "Organization access denied for orgId={}: jwtOrgId={} jwtRole={}",
                    organizationId,
                    TenantContext.getOrganizationId(),
                    TenantContext.getRole()
            );
        }

        return allowed;
    }

    private Optional<OrganizationMemberRole> resolveMembershipRole(Long organizationId) {
        try {
            User user = authenticatedUserService.getCurrentUser();

            return organizationMemberRepository
                    .findByUserAndOrganizationIdAndStatus(
                            user,
                            organizationId,
                            OrganizationMemberStatus.ACTIVE
                    )
                    .map(OrganizationMember::getRole);
        } catch (RuntimeException ex) {
            return Optional.empty();
        }
    }

    private OrganizationMemberRole resolveCurrentRole() {
        OrganizationMemberRole roleFromContext = TenantContext.getRole();
        if (roleFromContext != null) {
            return roleFromContext;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith("ORG_"))
                .findFirst()
                .map(authority -> OrganizationMemberRole.valueOf(authority.substring("ORG_".length())))
                .orElse(null);
    }
}
