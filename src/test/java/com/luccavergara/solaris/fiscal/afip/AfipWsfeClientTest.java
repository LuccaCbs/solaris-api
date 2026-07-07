package com.luccavergara.solaris.fiscal.afip;

import com.luccavergara.solaris.entity.CondicionIva;
import com.luccavergara.solaris.entity.TipoComprobante;
import com.luccavergara.solaris.fiscal.EmitInvoiceCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class AfipWsfeClientTest {

    private AfipWsfeClient client;
    private int soapCallCount;

    @BeforeEach
    void setUp() {
        soapCallCount = 0;
        AfipProperties properties = new AfipProperties();
        properties.getHomologation().setEnabled(true);

        AfipWsaaClient wsaaClient = new AfipWsaaClient(
                properties,
                (url, action, envelope) -> "",
                null,
                new AfipTokenCache()
        ) {
            @Override
            public AfipAuthToken authenticate(AfipCredentials credentials, String cuit) {
                return new AfipAuthToken(
                        "TOKEN",
                        "SIGN",
                        java.time.Instant.now().plusSeconds(3600)
                );
            }
        };

        client = new AfipWsfeClient(
                properties,
                (url, action, envelope) -> {
                    soapCallCount++;
                    if (envelope.contains("FECompUltimoAutorizado")) {
                        return """
                                <soap:Envelope>
                                  <soap:Body>
                                    <FECompUltimoAutorizadoResponse>
                                      <FECompUltimoAutorizadoResult>
                                        <CbteNro>10</CbteNro>
                                      </FECompUltimoAutorizadoResult>
                                    </FECompUltimoAutorizadoResponse>
                                  </soap:Body>
                                </soap:Envelope>
                                """;
                    }
                    return """
                            <soap:Envelope>
                              <soap:Body>
                                <FECAESolicitarResponse>
                                  <FECAESolicitarResult>
                                    <FeCabResp>
                                      <Resultado>A</Resultado>
                                    </FeCabResp>
                                    <FeDetResp>
                                      <FECAEDetResponse>
                                        <CAE>71000000000001</CAE>
                                        <CAEFchVto>20260716</CAEFchVto>
                                      </FECAEDetResponse>
                                    </FeDetResp>
                                  </FECAESolicitarResult>
                                </FECAESolicitarResponse>
                              </soap:Body>
                            </soap:Envelope>
                            """;
                },
                wsaaClient
        );
    }

    @Test
    void authorizeInvoice_returnsCaeForFacturaC() {
        EmitInvoiceCommand command = EmitInvoiceCommand.builder()
                .emitterCuit("20123456789")
                .puntoVenta(1)
                .numeroComprobante(11L)
                .tipoComprobante(TipoComprobante.FACTURA_C)
                .customerDocumentType("OTRO")
                .customerDocumentNumber("0")
                .customerRazonSocial("Consumidor Final")
                .customerCondicionIva(CondicionIva.CONSUMIDOR_FINAL)
                .importeNeto(new BigDecimal("100.00"))
                .importeIva(BigDecimal.ZERO)
                .importeTotal(new BigDecimal("100.00"))
                .build();

        AfipInvoiceAuthorization result = client.authorizeInvoice(
                command,
                AfipCredentials.empty(),
                "20123456789"
        );

        assertThat(soapCallCount).isEqualTo(2);
        assertThat(result.isAuthorized()).isTrue();
        assertThat(result.getCae()).isEqualTo("71000000000001");
        assertThat(result.getCaeVencimiento()).isEqualTo(LocalDate.of(2026, 7, 16));
        assertThat(result.getNumeroComprobante()).isEqualTo(11L);
    }

    @Test
    void parseAuthorizationResponse_handlesRejection() {
        String response = """
                <soap:Envelope>
                  <soap:Body>
                    <FECAESolicitarResponse>
                      <FECAESolicitarResult>
                        <FeCabResp>
                          <Resultado>R</Resultado>
                        </FeCabResp>
                        <FeDetResp>
                          <FECAEDetResponse>
                            <Observaciones>
                              <Obs>
                                <Code>10016</Code>
                                <Msg>Invalid document</Msg>
                              </Obs>
                            </Observaciones>
                          </FECAEDetResponse>
                        </FeDetResp>
                      </FECAESolicitarResult>
                    </FECAESolicitarResponse>
                  </soap:Body>
                </soap:Envelope>
                """;

        AfipInvoiceAuthorization result = client.parseAuthorizationResponse(
                response,
                EmitInvoiceCommand.builder()
                        .tipoComprobante(TipoComprobante.FACTURA_C)
                        .puntoVenta(1)
                        .build(),
                1L
        );

        assertThat(result.isAuthorized()).isFalse();
        assertThat(result.getRejectionReason()).contains("Invalid document");
    }
}
