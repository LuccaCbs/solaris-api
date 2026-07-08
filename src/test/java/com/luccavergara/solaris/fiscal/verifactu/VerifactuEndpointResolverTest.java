package com.luccavergara.solaris.fiscal.verifactu;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VerifactuEndpointResolverTest {

    private final VerifactuEndpointResolver resolver = new VerifactuEndpointResolver();

    @Test
    void resolveServiceUrl_usesProductionWhenEnabled() {
        VerifactuProperties properties = new VerifactuProperties();
        properties.getProduction().setEnabled(true);

        assertThat(resolver.resolveServiceUrl(properties))
                .isEqualTo(VerifactuWsdlEndpoints.PRODUCTION_VERIFACTU);
    }

    @Test
    void resolveServiceUrl_usesSandboxByDefault() {
        VerifactuProperties properties = new VerifactuProperties();

        assertThat(resolver.resolveServiceUrl(properties))
                .isEqualTo(VerifactuWsdlEndpoints.SANDBOX_VERIFACTU);
    }

    @Test
    void resolveQrValidationBaseUrl_usesProductionWhenEnabled() {
        VerifactuProperties properties = new VerifactuProperties();
        properties.getProduction().setEnabled(true);

        assertThat(resolver.resolveQrValidationBaseUrl(properties))
                .isEqualTo(VerifactuWsdlEndpoints.PRODUCTION_QR_VALIDATION);
    }
}
