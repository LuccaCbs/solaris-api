package com.luccavergara.solaris.fiscal.afip;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.luccavergara.solaris.entity.CondicionIva;
import com.luccavergara.solaris.entity.TipoComprobante;
import com.luccavergara.solaris.fiscal.EmitInvoiceCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class AfipWsaaClientTest {

    private AfipWsaaClient client;

    @BeforeEach
    void setUp() {
        AfipProperties properties = new AfipProperties();
        properties.getHomologation().setEnabled(true);
        client = new AfipWsaaClient(
                properties,
                (url, action, envelope) -> "",
                null,
                new AfipTokenCache()
        );
    }

    @Test
    void buildLoginTicketRequest_containsWsfeService() {
        String tra = client.buildLoginTicketRequest();

        assertThat(tra).contains("<service>wsfe</service>");
        assertThat(tra).contains("<uniqueId>");
        assertThat(tra).contains("<generationTime>");
        assertThat(tra).contains("<expirationTime>");
    }

    @Test
    void parseLoginResponse_extractsTokenAndSign() {
        String response = """
                <soapenv:Envelope>
                  <soapenv:Body>
                    <loginCmsResponse>
                      <loginCmsReturn>
                        <loginTicketResponse>
                          <credentials>
                            <token>TEST_TOKEN</token>
                            <sign>TEST_SIGN</sign>
                          </credentials>
                          <header>
                            <expirationTime>2030-01-01T12:00:00</expirationTime>
                          </header>
                        </loginTicketResponse>
                      </loginCmsReturn>
                    </loginCmsResponse>
                  </soapenv:Body>
                </soapenv:Envelope>
                """;

        AfipAuthToken token = client.parseLoginResponse(response);

        assertThat(token.token()).isEqualTo("TEST_TOKEN");
        assertThat(token.sign()).isEqualTo("TEST_SIGN");
        assertThat(token.expirationTime()).isAfter(Instant.now());
        assertThat(token.isValid()).isTrue();
    }
}
