package com.luccavergara.solaris.controller;

import com.luccavergara.solaris.dto.RedeemPromoWithTokenRequest;
import com.luccavergara.solaris.dto.RedeemPromoCodeRequest;
import com.luccavergara.solaris.dto.RedeemPromoCodeResponse;
import com.luccavergara.solaris.dto.StoreAddonCheckoutRequest;
import com.luccavergara.solaris.dto.StoreAddonCheckoutResponse;
import com.luccavergara.solaris.dto.StoreAddonCheckoutWithTokenRequest;
import com.luccavergara.solaris.service.BillingSessionService;
import com.luccavergara.solaris.service.PromoCodeService;
import com.luccavergara.solaris.service.SubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/billing")
@RequiredArgsConstructor
public class BillingPublicController {

    private final BillingSessionService billingSessionService;
    private final SubscriptionService subscriptionService;
    private final PromoCodeService promoCodeService;

    @PostMapping("/store-addon/checkout")
    public StoreAddonCheckoutResponse checkout(
            @Valid @RequestBody StoreAddonCheckoutWithTokenRequest request
    ) {
        Long organizationId = billingSessionService.resolveOrganizationId(request.getBillingToken());

        return subscriptionService.initiateStoreAddonCheckout(
                organizationId,
                StoreAddonCheckoutRequest.builder()
                        .quantity(request.getQuantity())
                        .build()
        );
    }

    @PostMapping("/promo-codes/redeem")
    public RedeemPromoCodeResponse redeemPromoCode(
            @Valid @RequestBody RedeemPromoWithTokenRequest request
    ) {
        Long organizationId = billingSessionService.resolveOrganizationId(request.getBillingToken());

        return promoCodeService.redeemPromoCode(
                organizationId,
                RedeemPromoCodeRequest.builder()
                        .code(request.getCode())
                        .build()
        );
    }
}
