package com.luccavergara.solaris.controller;

import com.luccavergara.solaris.dto.RedeemPromoCodeRequest;
import com.luccavergara.solaris.dto.RedeemPromoCodeResponse;
import com.luccavergara.solaris.dto.StoreAddonCheckoutRequest;
import com.luccavergara.solaris.dto.StoreAddonCheckoutResponse;
import com.luccavergara.solaris.security.OrganizationSecurity;
import com.luccavergara.solaris.service.PromoCodeService;
import com.luccavergara.solaris.service.SubscriptionService;
import com.luccavergara.solaris.tenant.TenantContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me/billing")
@RequiredArgsConstructor
public class MeBillingController {

    private final SubscriptionService subscriptionService;
    private final PromoCodeService promoCodeService;
    private final OrganizationSecurity organizationSecurity;

    @PostMapping("/store-addon/checkout")
    public StoreAddonCheckoutResponse initiateStoreAddonCheckout(
            @Valid @RequestBody StoreAddonCheckoutRequest request
    ) {
        Long orgId = requireSessionOrganizationId();
        organizationSecurity.requireBillingAccess(orgId);
        return subscriptionService.initiateStoreAddonCheckout(orgId, request);
    }

    @PostMapping("/promo-codes/redeem")
    public RedeemPromoCodeResponse redeemPromoCode(
            @Valid @RequestBody RedeemPromoCodeRequest request
    ) {
        Long orgId = requireSessionOrganizationId();
        organizationSecurity.requireBillingAccess(orgId);
        return promoCodeService.redeemPromoCode(orgId, request);
    }

    private Long requireSessionOrganizationId() {
        Long orgId = TenantContext.getOrganizationId();

        if (orgId == null) {
            throw new AccessDeniedException(
                    "No organization selected in your session. Sign out, sign in again, and select the organization."
            );
        }

        return orgId;
    }
}
