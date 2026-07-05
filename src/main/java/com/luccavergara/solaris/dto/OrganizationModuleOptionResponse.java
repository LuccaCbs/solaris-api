package com.luccavergara.solaris.dto;

import com.luccavergara.solaris.entity.ModuleCode;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrganizationModuleOptionResponse {

    private ModuleCode code;
    private String displayName;
    private boolean includedInPlan;
    private boolean requiresOptIn;
    private boolean enabled;
}
