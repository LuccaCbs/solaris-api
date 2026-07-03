package com.luccavergara.solaris.dto;

import com.luccavergara.solaris.entity.ModuleCode;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrganizationEntitlementsResponse {

    private List<ModuleCode> activeModules;
    private List<ModuleCode> planModules;
    private List<ModuleCode> addonModules;
    private List<ModuleCode> promoModules;
}
