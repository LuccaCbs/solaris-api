package com.luccavergara.solaris.dto;

import com.luccavergara.solaris.entity.OrganizationMemberRole;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrganizationInvitePreviewResponse {

    private String organizationName;
    private String email;
    private OrganizationMemberRole role;
    private boolean existingUser;
    private boolean expired;
}
