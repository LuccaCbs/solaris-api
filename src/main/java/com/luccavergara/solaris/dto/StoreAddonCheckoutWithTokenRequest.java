package com.luccavergara.solaris.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreAddonCheckoutWithTokenRequest {

    @NotBlank
    private String billingToken;

    @Min(1)
    @Max(10)
    @Builder.Default
    private Integer quantity = 1;
}
