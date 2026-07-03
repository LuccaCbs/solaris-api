package com.luccavergara.solaris.dto;

import com.luccavergara.solaris.entity.BillingJurisdiction;
import com.luccavergara.solaris.entity.BillingProvider;
import com.luccavergara.solaris.entity.CountryCode;
import com.luccavergara.solaris.entity.ModuleCode;
import com.luccavergara.solaris.entity.SubscriptionPlanCode;
import com.luccavergara.solaris.entity.SubscriptionStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrganizationSubscriptionResponse {

    private SubscriptionPlanCode planCode;
    private String planDisplayName;
    private SubscriptionStatus status;
    private Integer maxStores;
    private Integer extraStoresPurchased;
    private Integer allowedStores;
    private Long activeStoreCount;
    private Boolean canAddStore;
    private BillingProvider billingProvider;
    private BillingProvider preferredBillingProvider;
    private String paymentProviderDisplayName;
    private CountryCode countryCode;
    private BillingJurisdiction billingJurisdiction;
    private String defaultCurrency;
    private LocalDateTime trialEndsAt;
    private LocalDateTime currentPeriodStart;
    private LocalDateTime currentPeriodEnd;
    private List<ModuleCode> activeModules;
    private List<ModuleCode> planModules;
    private List<ModuleCode> addonModules;
    private List<ModuleCode> promoModules;
}
