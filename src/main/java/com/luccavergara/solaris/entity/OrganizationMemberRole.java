package com.luccavergara.solaris.entity;

public enum OrganizationMemberRole {
    OWNER(5),
    ADMIN(4),
    MANAGER(3),
    REPOSITOR(2),
    CASHIER(1);

    private final int privilegeLevel;

    OrganizationMemberRole(int privilegeLevel) {
        this.privilegeLevel = privilegeLevel;
    }

    public int getPrivilegeLevel() {
        return privilegeLevel;
    }
}
