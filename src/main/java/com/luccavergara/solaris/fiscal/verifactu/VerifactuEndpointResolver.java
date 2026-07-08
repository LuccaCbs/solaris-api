package com.luccavergara.solaris.fiscal.verifactu;

import org.springframework.stereotype.Component;

@Component
public class VerifactuEndpointResolver {

    public boolean isSubmissionEnabled(VerifactuProperties properties) {
        return properties.getProduction().isEnabled() || properties.getSandbox().isEnabled();
    }

    public String resolveServiceUrl(VerifactuProperties properties) {
        if (properties.getProduction().isEnabled()) {
            return properties.getProduction().getServiceUrl();
        }
        return properties.getSandbox().getServiceUrl();
    }

    public String resolveQrValidationBaseUrl(VerifactuProperties properties) {
        if (properties.getProduction().isEnabled()) {
            return properties.getProduction().getQrValidationBaseUrl();
        }
        return properties.getSandbox().getQrValidationBaseUrl();
    }

    public String resolveEnvironmentLabel(VerifactuProperties properties) {
        return properties.getProduction().isEnabled() ? "production" : "sandbox";
    }
}
