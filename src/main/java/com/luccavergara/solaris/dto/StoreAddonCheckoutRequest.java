package com.luccavergara.solaris.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreAddonCheckoutRequest {

    @Min(1)
    @Max(10)
    @Builder.Default
    private Integer quantity = 1;
}
