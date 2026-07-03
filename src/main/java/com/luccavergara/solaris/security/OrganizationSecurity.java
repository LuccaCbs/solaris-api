package com.luccavergara.solaris.security;

import com.luccavergara.solaris.entity.OrganizationMember;
import com.luccavergara.solaris.entity.OrganizationMemberRole;
import com.luccavergara.solaris.entity.OrganizationMemberStatus;
import com.luccavergara.solaris.entity.User;
import com.luccavergara.solaris.repository.OrganizationMemberRepository;
import com.luccavergara.solaris.repository.UserRepository;
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

    private final UserRepository userRepository;
    private final OrganizationMemberRepository organizationMemberRepository;

    public boolean hasMinimumRole(OrganizationMemberRole minimumRole) {
        OrganizationMemberRole currentRole = resolveCurrentRole();

        if (currentRole == null) {
            return false;
        }

        return currentRole.getPrivilegeLevel() >= minimumRole.getPrivilegeLevel();
    }

    public boolean belongsToOrganization(Object organizationId) {
        Long orgId = toOrganizationId(organizationId);
        Long currentOrgId = TenantContext.getOrganizationId();

        if (orgId == null || currentOrgId == null) {
            return false;
        }

        return currentOrgId.longValue() == orgId.longValue();
    }

    public boolean canAccessOrganization(Object organizationId, OrganizationMemberRole minimumRole) {
        Long orgId = toOrganizationId(organizationId);

        if (orgId == null) {
            log.warn("Organization access denied: unresolved organization id {}", organizationId);
            return false;
        }

        Optional<OrganizationMemberRole> membershipRole = resolveMembershipRole(orgId);

        if (membershipRole.isPresent()) {
            boolean allowed = membershipRole.get().getPrivilegeLevel() >= minimumRole.getPrivilegeLevel();

            if (!allowed) {
                log.warn(
                        "Organization access denied for orgId={}: role {} below minimum {}",
                        orgId,
                        membershipRole.get(),
                        minimumRole
                );
            }

            return allowed;
        }

        boolean allowed = belongsToOrganization(orgId) && hasMinimumRole(minimumRole);

        if (!allowed) {
            log.warn(
                    "Organization access denied for orgId={}: jwtOrgId={} jwtRole={}",
                    orgId,
                    TenantContext.getOrganizationId(),
                    TenantContext.getRole()
            );
        }

        return allowed;
    }

    private Optional<OrganizationMemberRole> resolveMembershipRole(Long organizationId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }

        Optional<User> user = resolveAuthenticatedUser(authentication);

        if (user.isEmpty()) {
            return Optional.empty();
        }

        return organizationMemberRepository
                .findByUserAndOrganizationIdAndStatus(
                        user.get(),
                        organizationId,
                        OrganizationMemberStatus.ACTIVE
                )
                .map(OrganizationMember::getRole);
    }

    private Optional<User> resolveAuthenticatedUser(Authentication authentication) {
        Object principal = authentication.getPrincipal();

        if (principal instanceof User user) {
            return Optional.of(user);
        }

        String email = authentication.getName();

        if (email == null || email.isBlank()) {
            return Optional.empty();
        }

        return userRepository.findByEmail(email);
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

    private Long toOrganizationId(Object organizationId) {
        if (organizationId == null) {
            return null;
        }

        if (organizationId instanceof Number number) {
            return number.longValue();
        }

        if (organizationId instanceof String string && !string.isBlank()) {
            return Long.parseLong(string.trim());
        }

        return null;
    }
}
