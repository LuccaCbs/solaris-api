package com.luccavergara.solaris.dto;

import com.luccavergara.solaris.entity.OrganizationMemberRole;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateOrganizationMemberRoleRequest {

    @NotNull
    private OrganizationMemberRole role;

    private Long storeId;
}
