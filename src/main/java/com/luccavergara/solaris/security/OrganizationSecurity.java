package com.luccavergara.solaris.security;

import com.luccavergara.solaris.entity.OrganizationMemberRole;
import com.luccavergara.solaris.tenant.TenantContext;
import org.springframework.stereotype.Component;

@Component("organizationSecurity")
public class OrganizationSecurity {

    public boolean hasMinimumRole(OrganizationMemberRole minimumRole) {
        OrganizationMemberRole currentRole = TenantContext.getRole();

        if (currentRole == null) {
            return true;
        }

        return currentRole.getPrivilegeLevel() >= minimumRole.getPrivilegeLevel();
    }
}
