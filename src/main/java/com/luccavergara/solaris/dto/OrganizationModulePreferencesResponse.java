package com.luccavergara.solaris.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrganizationModulePreferencesResponse {

    private List<OrganizationModuleOptionResponse> modules;
}
