package com.luccavergara.solaris.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SelectOrganizationRequest {

    @NotNull
    private Long organizationId;

    private Long storeId;
}
