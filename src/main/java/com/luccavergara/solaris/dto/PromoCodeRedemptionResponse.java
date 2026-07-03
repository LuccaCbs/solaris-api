package com.luccavergara.solaris.dto;

import com.luccavergara.solaris.entity.ModuleCode;
import com.luccavergara.solaris.entity.PromoRedemptionStatus;
import com.luccavergara.solaris.entity.SubscriptionPlanCode;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromoCodeRedemptionResponse {

    private Long id;
    private Long promoCodeId;
    private String promoCode;
    private Long organizationId;
    private String organizationName;
    private Long redeemedByUserId;
    private PromoRedemptionStatus status;
    private SubscriptionPlanCode grantedPlanCode;
    private ModuleCode grantedModuleCode;
    private LocalDateTime accessValidFrom;
    private LocalDateTime accessValidUntil;
    private LocalDateTime createdAt;
    private LocalDateTime revokedAt;
}
