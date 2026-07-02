package com.luccavergara.solaris.tenant;

import com.luccavergara.solaris.entity.OrganizationMemberRole;
import lombok.experimental.UtilityClass;

@UtilityClass
public class TenantContext {

    private static final ThreadLocal<Long> ORGANIZATION_ID = new ThreadLocal<>();
    private static final ThreadLocal<OrganizationMemberRole> ROLE = new ThreadLocal<>();
    private static final ThreadLocal<Long> STORE_ID = new ThreadLocal<>();

    public static void setOrganizationId(Long organizationId) {
        ORGANIZATION_ID.set(organizationId);
    }

    public static Long getOrganizationId() {
        return ORGANIZATION_ID.get();
    }

    public static void setRole(OrganizationMemberRole role) {
        ROLE.set(role);
    }

    public static OrganizationMemberRole getRole() {
        return ROLE.get();
    }

    public static void setStoreId(Long storeId) {
        STORE_ID.set(storeId);
    }

    public static Long getStoreId() {
        return STORE_ID.get();
    }

    public static void clear() {
        ORGANIZATION_ID.remove();
        ROLE.remove();
        STORE_ID.remove();
    }
}
