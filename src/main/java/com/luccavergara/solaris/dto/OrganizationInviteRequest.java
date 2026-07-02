package com.luccavergara.solaris.dto;

import com.luccavergara.solaris.entity.OrganizationMemberRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrganizationInviteRequest {

    @NotBlank
    @Email
    private String email;

    @NotNull
    private OrganizationMemberRole role;

    private Long storeId;
}
