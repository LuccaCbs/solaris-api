package com.luccavergara.solaris.billing;

import com.luccavergara.solaris.entity.BillingJurisdiction;
import com.luccavergara.solaris.entity.Organization;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class BillingPricingService {

    @Value("${application.billing.store-addon-price-ars:15000}")
    private BigDecimal storeAddonPriceArs;

    @Value("${application.billing.store-addon-price-eur:15}")
    private BigDecimal storeAddonPriceEur;

    public BillingPrice resolveStoreAddonPrice(Organization organization) {
        BillingJurisdiction jurisdiction = organization.getBillingJurisdiction();

        if (jurisdiction == BillingJurisdiction.EU) {
            return new BillingPrice(storeAddonPriceEur, "EUR");
        }

        return new BillingPrice(storeAddonPriceArs, "ARS");
    }
}
