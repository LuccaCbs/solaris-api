package com.luccavergara.solaris.dto;

import com.luccavergara.solaris.entity.BillingProvider;
import com.luccavergara.solaris.entity.SubscriptionPlanCode;
import com.luccavergara.solaris.entity.SubscriptionStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrganizationSubscriptionResponse {

    private SubscriptionPlanCode planCode;
    private SubscriptionStatus status;
    private Integer maxStores;
    private Integer extraStoresPurchased;
    private Integer allowedStores;
    private Long activeStoreCount;
    private Boolean canAddStore;
    private BillingProvider billingProvider;
    private LocalDateTime trialEndsAt;
    private LocalDateTime currentPeriodStart;
    private LocalDateTime currentPeriodEnd;
}
