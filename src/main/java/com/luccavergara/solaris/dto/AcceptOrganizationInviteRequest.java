package com.luccavergara.solaris.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AcceptOrganizationInviteRequest {

    @NotBlank
    private String token;

    private String password;

    private String firstname;

    private String lastname;
}
