package com.luccavergara.solaris.fiscal.afip;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.luccavergara.solaris.entity.CondicionIva;
import com.luccavergara.solaris.entity.TipoComprobante;
import com.luccavergara.solaris.fiscal.EmitInvoiceCommand;
import com.luccavergara.solaris.fiscal.EmitInvoiceResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AfipNativeFiscalProviderTest {

    private AfipNativeFiscalProvider provider;
    private AfipProperties properties;

    @BeforeEach
    void setUp() {
        properties = new AfipProperties();
        properties.getHomologation().setEnabled(true);

        AfipWsfeClient wsfeClient = new AfipWsfeClient(
                properties,
                (url, action, envelope) -> """
                        <soap:Envelope>
                          <soap:Body>
                            <FECAESolicitarResponse>
                              <FECAESolicitarResult>
                                <FeCabResp><Resultado>A</Resultado></FeCabResp>
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
                        """,
                new AfipWsaaClient(
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
                }
        ) {
            @Override
            long fetchUltimoAutorizado(AfipAuthToken auth, String cuit, int puntoVenta, int cbteTipo) {
                return 0L;
            }
        };

        provider = new AfipNativeFiscalProvider(properties, wsfeClient, new ObjectMapper());
    }

    @Test
    void emitInvoice_authorizesFacturaC() {
        EmitInvoiceCommand command = facturaCCommand();

        EmitInvoiceResult result = provider.emitInvoice(command, AfipCredentials.empty());

        assertThat(result.isAuthorized()).isTrue();
        assertThat(result.getCae()).isEqualTo("71000000000001");
        assertThat(result.getCaeVencimiento()).isEqualTo(LocalDate.of(2026, 7, 16));
        assertThat(result.getNumeroComprobante()).isEqualTo(1L);
    }

    @Test
    void emitInvoice_rejectsWhenHomologationDisabled() {
        properties.getHomologation().setEnabled(false);

        EmitInvoiceResult result = provider.emitInvoice(facturaCCommand(), AfipCredentials.empty());

        assertThat(result.isAuthorized()).isFalse();
        assertThat(result.getRejectionReason()).contains("homologation is disabled");
    }

    @Test
    void emitInvoice_rejectsFacturaB() {
        EmitInvoiceCommand command = EmitInvoiceCommand.builder()
                .emitterCuit("20123456789")
                .puntoVenta(1)
                .numeroComprobante(1L)
                .tipoComprobante(TipoComprobante.FACTURA_B)
                .importeTotal(new BigDecimal("150.00"))
                .build();

        EmitInvoiceResult result = provider.emitInvoice(command, AfipCredentials.empty());

        assertThat(result.isAuthorized()).isFalse();
        assertThat(result.getRejectionReason()).contains("Factura C only");
    }

    @Test
    void emitCreditNote_isUnsupportedInPoc() {
        assertThatThrownBy(() -> provider.emitCreditNote(null))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("Phase 1");
    }

    private EmitInvoiceCommand facturaCCommand() {
        return EmitInvoiceCommand.builder()
                .emitterCuit("20123456789")
                .puntoVenta(1)
                .numeroComprobante(1L)
                .tipoComprobante(TipoComprobante.FACTURA_C)
                .customerDocumentType("OTRO")
                .customerDocumentNumber("0")
                .customerRazonSocial("Consumidor Final")
                .customerCondicionIva(CondicionIva.CONSUMIDOR_FINAL)
                .importeNeto(new BigDecimal("150.00"))
                .importeIva(BigDecimal.ZERO)
                .importeTotal(new BigDecimal("150.00"))
                .build();
    }
}
