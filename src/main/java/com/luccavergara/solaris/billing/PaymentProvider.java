package com.luccavergara.solaris.billing;

import com.luccavergara.solaris.entity.BillingProvider;
import com.luccavergara.solaris.entity.Organization;
import com.luccavergara.solaris.entity.PaymentMethodType;
import com.luccavergara.solaris.dto.StoreAddonCheckoutResponse;

import java.util.List;

public interface PaymentProvider {

    BillingProvider providerType();

    String displayName();

    List<PaymentMethodType> supportedPaymentMethods();

    boolean isConfigured();

    String notConfiguredMessage();

    StoreAddonCheckoutResponse createStoreAddonCheckout(Organization organization, int quantity);
}
