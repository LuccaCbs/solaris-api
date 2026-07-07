package com.luccavergara.solaris.fiscal.afip;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Optional live homologation test. Requires a real AFIP PKCS12 certificate and env vars.
 * Skipped in CI unless {@code AFIP_INTEGRATION_TEST_ENABLED=true}.
 */
@EnabledIfEnvironmentVariable(named = "AFIP_INTEGRATION_TEST_ENABLED", matches = "true")
class AfipHomologationIntegrationTest {

    @Test
    void wsaaAuthenticate_obtainsToken() {
        AfipProperties properties = buildPropertiesFromEnvironment();
        AfipCertificateLoader certificateLoader = new AfipCertificateLoader(properties);
        AfipWsaaClient wsaaClient = new AfipWsaaClient(
                properties,
                new AfipHttpSoapTransport(),
                certificateLoader,
                new AfipTokenCache()
        );

        AfipAuthToken token = wsaaClient.authenticate(AfipCredentials.empty(), properties.getCuit());

        assertThat(token.token()).isNotBlank();
        assertThat(token.sign()).isNotBlank();
        assertThat(token.isValid()).isTrue();
    }

    private AfipProperties buildPropertiesFromEnvironment() {
        AfipProperties properties = new AfipProperties();
        properties.getHomologation().setEnabled(true);
        properties.setCuit(requiredEnv("AFIP_CUIT"));
        properties.getCert().setPath(System.getenv("AFIP_CERT_PATH"));
        properties.getCert().setPassword(requiredEnv("AFIP_CERT_PASSWORD"));
        properties.getCert().setBase64(System.getenv("AFIP_CERT_BASE64"));
        return properties;
    }

    private String requiredEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required env var for AFIP integration test: " + name);
        }
        return value;
    }
}
