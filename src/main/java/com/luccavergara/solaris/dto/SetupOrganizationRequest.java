package com.luccavergara.solaris.dto;

import com.luccavergara.solaris.entity.CountryCode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SetupOrganizationRequest {

    @NotNull
    private CountryCode countryCode;

    @NotBlank
    @Size(max = 120)
    private String organizationName;

    @Size(max = 120)
    private String storeName;
}
