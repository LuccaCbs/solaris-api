package com.luccavergara.solaris.fiscal.afip;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class AfipCredentialsTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void parse_returnsEmptyWhenApiKeyMissing() {
        Optional<AfipCredentials> credentials = AfipCredentials.parse(null, objectMapper);

        assertThat(credentials).isPresent();
        assertThat(credentials.get().cuit()).isNull();
    }

    @Test
    void parse_readsAfipNativeJson() {
        String json = """
                {
                  "provider": "afip_native",
                  "cuit": "20123456789",
                  "puntoVenta": 3,
                  "certPath": "/secrets/afip.p12",
                  "certPassword": "secret"
                }
                """;

        Optional<AfipCredentials> credentials = AfipCredentials.parse(json, objectMapper);

        assertThat(credentials).isPresent();
        assertThat(credentials.get().cuit()).isEqualTo("20123456789");
        assertThat(credentials.get().puntoVenta()).isEqualTo(3);
        assertThat(credentials.get().certPath()).isEqualTo("/secrets/afip.p12");
        assertThat(credentials.get().hasCertificateReference()).isTrue();
    }

    @Test
    void parse_ignoresTusFacturasJson() {
        String json = """
                {"apikey":"k","apitoken":"t","usertoken":"u"}
                """;

        Optional<AfipCredentials> credentials = AfipCredentials.parse(json, objectMapper);

        assertThat(credentials).isEmpty();
    }
}
