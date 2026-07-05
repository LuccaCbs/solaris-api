package com.luccavergara.solaris.dto;

import com.luccavergara.solaris.entity.ModuleCode;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateOrganizationModulePreferencesRequest {

    @NotNull
    private List<ModuleCode> enabledModules;
}
