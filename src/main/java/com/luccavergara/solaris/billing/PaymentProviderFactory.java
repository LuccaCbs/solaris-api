package com.luccavergara.solaris.billing;

import com.luccavergara.solaris.entity.BillingJurisdiction;
import com.luccavergara.solaris.entity.BillingProvider;
import com.luccavergara.solaris.entity.Organization;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentProviderFactory {

    private final MercadoPagoPaymentProvider mercadoPagoPaymentProvider;
    private final StripePaymentProvider stripePaymentProvider;

    public PaymentProvider resolve(Organization organization) {
        BillingJurisdiction jurisdiction = organization.getBillingJurisdiction();

        if (jurisdiction == null) {
            return mercadoPagoPaymentProvider;
        }

        return switch (jurisdiction) {
            case AR -> mercadoPagoPaymentProvider;
            case EU -> stripePaymentProvider;
        };
    }

    public BillingProvider resolveProviderType(Organization organization) {
        return resolve(organization).providerType();
    }
}
