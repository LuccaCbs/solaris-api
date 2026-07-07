package com.luccavergara.solaris.fiscal.verifactu;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VerifactuCredentialsTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void parse_readsVerifactuNativeCredentials() {
        String json = """
                {
                  "provider": "verifactu_native",
                  "nif": "B12345678",
                  "serie": 3,
                  "certBase64": "abc",
                  "certPassword": "secret"
                }
                """;

        VerifactuCredentials credentials = VerifactuCredentials.parse(json, objectMapper).orElseThrow();

        assertThat(credentials.nif()).isEqualTo("B12345678");
        assertThat(credentials.serie()).isEqualTo(3);
        assertThat(credentials.hasCertificateReference()).isTrue();
    }

    @Test
    void parse_ignoresTusFacturasPayload() {
        String json = """
                {"apikey":"x","apitoken":"y","usertoken":"z"}
                """;

        assertThat(VerifactuCredentials.parse(json, objectMapper)).isEmpty();
    }
}
