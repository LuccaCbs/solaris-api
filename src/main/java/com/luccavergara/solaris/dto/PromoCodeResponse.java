package com.luccavergara.solaris.dto;

import com.luccavergara.solaris.entity.ModuleCode;
import com.luccavergara.solaris.entity.PromoCodeStatus;
import com.luccavergara.solaris.entity.PromoCodeType;
import com.luccavergara.solaris.entity.SubscriptionPlanCode;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromoCodeResponse {

    private Long id;
    private String code;
    private PromoCodeType promoType;
    private SubscriptionPlanCode grantPlanCode;
    private ModuleCode grantModuleCode;
    private Integer durationDays;
    private Integer maxRedemptions;
    private Integer redemptionCount;
    private PromoCodeStatus status;
    private LocalDateTime validFrom;
    private LocalDateTime validUntil;
    private String internalNote;
    private Long createdByUserId;
    private LocalDateTime revokedAt;
    private String revokeReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean redeemable;
}
