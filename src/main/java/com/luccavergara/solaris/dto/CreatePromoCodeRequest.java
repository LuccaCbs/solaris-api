package com.luccavergara.solaris.dto;

import com.luccavergara.solaris.entity.ModuleCode;
import com.luccavergara.solaris.entity.PromoCodeType;
import com.luccavergara.solaris.entity.SubscriptionPlanCode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatePromoCodeRequest {

    @NotBlank
    @Size(max = 64)
    private String code;

    @NotNull
    private PromoCodeType promoType;

    private SubscriptionPlanCode grantPlanCode;

    private ModuleCode grantModuleCode;

    @Positive
    private Integer durationDays;

    @Positive
    private Integer maxRedemptions;

    private LocalDateTime validFrom;

    private LocalDateTime validUntil;

    private String internalNote;
}
