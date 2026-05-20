package com.luccavergara.solaris.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemSettingsRequest {

    @NotNull
    @Min(0)
    private Integer globalLowStockThreshold;
}