package com.luccavergara.solaris.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AccessContextResponse {
    String email;
    boolean authenticated;
    Long jwtOrgId;
    String jwtRole;
    Long tenantOrgId;
    String tenantRole;
    Long requestedOrgId;
    String membershipRole;
    String membershipStatus;
    boolean canAccessAsAdmin;
    String denialReason;
}
