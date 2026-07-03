package com.luccavergara.solaris.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RedeemPromoCodeRequest {

    @NotBlank
    @Size(max = 64)
    private String code;
}
