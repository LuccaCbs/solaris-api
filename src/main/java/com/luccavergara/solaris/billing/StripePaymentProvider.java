package com.luccavergara.solaris.billing;

import com.luccavergara.solaris.dto.StoreAddonCheckoutResponse;
import com.luccavergara.solaris.entity.BillingProvider;
import com.luccavergara.solaris.entity.Organization;
import com.luccavergara.solaris.entity.PaymentMethodType;
import com.luccavergara.solaris.service.StripeBillingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class StripePaymentProvider implements PaymentProvider {

    private final StripeBillingService stripeBillingService;
    private final BillingPricingService billingPricingService;

    @Override
    public BillingProvider providerType() {
        return BillingProvider.STRIPE;
    }

    @Override
    public String displayName() {
        return "Stripe";
    }

    @Override
    public List<PaymentMethodType> supportedPaymentMethods() {
        return List.of(
                PaymentMethodType.CARD,
                PaymentMethodType.GOOGLE_PAY,
                PaymentMethodType.APPLE_PAY
        );
    }

    @Override
    public boolean isConfigured() {
        return stripeBillingService.isConfigured();
    }

    @Override
    public String notConfiguredMessage() {
        return "Stripe is not configured. Set STRIPE_SECRET_KEY to enable card and wallet payments.";
    }

    @Override
    public StoreAddonCheckoutResponse createStoreAddonCheckout(Organization organization, int quantity) {
        StoreAddonCheckoutResponse response = stripeBillingService.createStoreAddonCheckout(organization, quantity);
        BillingPrice price = billingPricingService.resolveStoreAddonPrice(organization);

        response.setProvider(providerType());
        response.setProviderDisplayName(displayName());
        response.setSupportedPaymentMethods(supportedPaymentMethods());
        response.setCurrency(price.currency());
        response.setUnitPrice(price.unitAmount());

        return response;
    }
}
