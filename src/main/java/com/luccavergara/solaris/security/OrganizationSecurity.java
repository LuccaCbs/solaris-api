package com.luccavergara.solaris.security;

import com.luccavergara.solaris.entity.OrganizationMemberRole;
import com.luccavergara.solaris.tenant.TenantContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component("organizationSecurity")
public class OrganizationSecurity {

    public boolean hasMinimumRole(OrganizationMemberRole minimumRole) {
        OrganizationMemberRole currentRole = resolveCurrentRole();

        if (currentRole == null) {
            return false;
        }

        return currentRole.getPrivilegeLevel() >= minimumRole.getPrivilegeLevel();
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
