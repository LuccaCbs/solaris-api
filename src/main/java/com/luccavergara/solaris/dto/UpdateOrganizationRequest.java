package com.luccavergara.solaris.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateOrganizationRequest {

    @NotBlank
    @Size(max = 255)
    private String displayName;
}
