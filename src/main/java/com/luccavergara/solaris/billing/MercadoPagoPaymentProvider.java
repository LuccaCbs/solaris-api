package com.luccavergara.solaris.billing;

import com.luccavergara.solaris.dto.StoreAddonCheckoutResponse;
import com.luccavergara.solaris.entity.BillingProvider;
import com.luccavergara.solaris.entity.Organization;
import com.luccavergara.solaris.entity.PaymentMethodType;
import com.luccavergara.solaris.service.MercadoPagoBillingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class MercadoPagoPaymentProvider implements PaymentProvider {

    private final MercadoPagoBillingService mercadoPagoBillingService;
    private final BillingPricingService billingPricingService;

    @Override
    public BillingProvider providerType() {
        return BillingProvider.MERCADOPAGO;
    }

    @Override
    public String displayName() {
        return "Mercado Pago";
    }

    @Override
    public List<PaymentMethodType> supportedPaymentMethods() {
        return List.of(PaymentMethodType.CARD);
    }

    @Override
    public boolean isConfigured() {
        return mercadoPagoBillingService.isConfigured();
    }

    @Override
    public String notConfiguredMessage() {
        return "Mercado Pago is not configured. Set MERCADOPAGO_ACCESS_TOKEN to enable card payments.";
    }

    @Override
    public StoreAddonCheckoutResponse createStoreAddonCheckout(Organization organization, int quantity) {
        StoreAddonCheckoutResponse response = mercadoPagoBillingService.createStoreAddonCheckout(
                organization.getId(),
                quantity
        );

        return enrich(response, organization);
    }

    private StoreAddonCheckoutResponse enrich(StoreAddonCheckoutResponse response, Organization organization) {
        BillingPrice price = billingPricingService.resolveStoreAddonPrice(organization);

        response.setProvider(providerType());
        response.setProviderDisplayName(displayName());
        response.setSupportedPaymentMethods(supportedPaymentMethods());
        response.setCurrency(price.currency());
        response.setUnitPrice(price.unitAmount());

        if (price.isArs()) {
            response.setUnitPriceArs(price.unitAmount());
        }

        return response;
    }
}
