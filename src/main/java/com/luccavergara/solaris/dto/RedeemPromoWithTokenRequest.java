package com.luccavergara.solaris.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RedeemPromoWithTokenRequest {

    @NotBlank
    private String billingToken;

    @NotBlank
    private String code;
}
