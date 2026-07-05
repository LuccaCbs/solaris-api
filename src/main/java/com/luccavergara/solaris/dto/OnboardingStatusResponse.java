package com.luccavergara.solaris.dto;

import com.luccavergara.solaris.entity.CountryCode;
import com.luccavergara.solaris.entity.SubscriptionStatus;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class OnboardingStatusResponse {

    boolean emailVerified;
    boolean needsOrganizationSetup;
    boolean needsPlanSelection;
    Long organizationId;
    String organizationName;
    CountryCode countryCode;
    SubscriptionStatus subscriptionStatus;
}
