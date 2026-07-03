package com.luccavergara.solaris.service;

import com.luccavergara.solaris.entity.*;
import org.springframework.stereotype.Service;

@Service
public class OrganizationJurisdictionService {

    public void applyCountryDefaults(Organization organization, CountryCode countryCode) {
        organization.setCountryCode(countryCode);

        switch (countryCode) {
            case ES -> {
                organization.setBillingJurisdiction(BillingJurisdiction.EU);
                organization.setFiscalJurisdiction(FiscalJurisdiction.ES_VERIFACTU);
                organization.setDefaultCurrency("EUR");

                if ("America/Argentina/Buenos_Aires".equals(organization.getTimezone())) {
                    organization.setTimezone("Europe/Madrid");
                }
            }
            case AR -> {
                organization.setBillingJurisdiction(BillingJurisdiction.AR);
                organization.setFiscalJurisdiction(FiscalJurisdiction.AR_AFIP);
                organization.setDefaultCurrency("ARS");
            }
        }
    }
}
