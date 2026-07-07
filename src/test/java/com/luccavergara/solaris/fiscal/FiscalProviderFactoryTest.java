package com.luccavergara.solaris.fiscal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.luccavergara.solaris.entity.FiscalJurisdiction;
import com.luccavergara.solaris.entity.FiscalProviderType;
import com.luccavergara.solaris.entity.Organization;
import com.luccavergara.solaris.fiscal.afip.AfipNativeFiscalProvider;
import com.luccavergara.solaris.fiscal.afip.AfipProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FiscalProviderFactoryTest {

    private FiscalProviderFactory factory;

    @BeforeEach
    void setUp() {
        AfipProperties afipProperties = new AfipProperties();
        afipProperties.getHomologation().setEnabled(true);
        afipProperties.getCert().setPath("/secrets/afip-homo.p12");

        factory = new FiscalProviderFactory(
                new MockFiscalProvider(new ObjectMapper()),
                new TusFacturasFiscalProvider(new ObjectMapper()),
                new AfipNativeFiscalProvider(afipProperties, null, new ObjectMapper()),
                afipProperties,
                new ObjectMapper()
        );
    }

    @Test
    void resolve_returnsAfipNativeWhenConfiguredWithCertificate() {
        Organization organization = Organization.builder()
                .id(1L)
                .fiscalProvider(FiscalProviderType.AFIP_NATIVE)
                .fiscalJurisdiction(FiscalJurisdiction.AR_AFIP)
                .build();

        FiscalProvider provider = factory.resolve(organization);

        assertThat(provider).isNotInstanceOf(MockFiscalProvider.class);
    }

    @Test
    void resolve_fallsBackToMockWhenCertificateMissing() {
        AfipProperties propertiesWithoutCert = new AfipProperties();
        FiscalProviderFactory factoryWithoutCert = new FiscalProviderFactory(
                new MockFiscalProvider(new ObjectMapper()),
                new TusFacturasFiscalProvider(new ObjectMapper()),
                new AfipNativeFiscalProvider(propertiesWithoutCert, null, new ObjectMapper()),
                propertiesWithoutCert,
                new ObjectMapper()
        );

        Organization organization = Organization.builder()
                .id(2L)
                .fiscalProvider(FiscalProviderType.AFIP_NATIVE)
                .fiscalJurisdiction(FiscalJurisdiction.AR_AFIP)
                .build();

        FiscalProvider provider = factoryWithoutCert.resolve(organization);

        assertThat(provider).isInstanceOf(MockFiscalProvider.class);
    }
}
