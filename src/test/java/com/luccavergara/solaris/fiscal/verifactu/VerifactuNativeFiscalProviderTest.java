package com.luccavergara.solaris.fiscal.verifactu;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.luccavergara.solaris.entity.TipoComprobante;
import com.luccavergara.solaris.fiscal.EmitCreditNoteCommand;
import com.luccavergara.solaris.fiscal.EmitInvoiceCommand;
import com.luccavergara.solaris.fiscal.EmitInvoiceResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VerifactuNativeFiscalProviderTest {

    @Mock
    private VerifactuAeatClient aeatClient;

    @Mock
    private VerifactuHashChainService hashChainService;

    private VerifactuNativeFiscalProvider provider;

    @BeforeEach
    void setUp() {
        VerifactuProperties properties = new VerifactuProperties();
        properties.getSandbox().setEnabled(true);
        provider = new VerifactuNativeFiscalProvider(
                properties,
                aeatClient,
                hashChainService,
                new ObjectMapper()
        );
    }

    @Test
    void emitInvoice_returnsAuthorizedResultWithHuella() {
        EmitInvoiceCommand command = EmitInvoiceCommand.builder()
                .emitterCuit("B12345678")
                .emitterRazonSocial("Solaris ES SL")
                .puntoVenta(1)
                .tipoComprobante(TipoComprobante.FACTURA_B)
                .numeroComprobante(10L)
                .importeNeto(new BigDecimal("100.00"))
                .importeIva(new BigDecimal("21.00"))
                .importeTotal(new BigDecimal("121.00"))
                .customerDocumentNumber("0")
                .customerRazonSocial("Cliente final")
                .build();

        when(hashChainService.resolvePreviousHash(7L)).thenReturn("");
        when(aeatClient.submitAlta(any(), any(), eq("B12345678"), eq("Solaris ES SL"), eq("")))
                .thenReturn(VerifactuInvoiceAuthorization.authorized(
                        TipoComprobante.FACTURA_B,
                        1,
                        10L,
                        "ABCDEF1234567890",
                        "https://prewww2.aeat.es/wlpl/TIKE-CONT/ValidarQR?nif=B12345678",
                        "<request/>",
                        "<response/>"
                ));

        EmitInvoiceResult result = provider.emitInvoice(command, VerifactuCredentials.empty(), 7L);

        assertThat(result.isAuthorized()).isTrue();
        assertThat(result.getCae()).isEqualTo("ABCDEF1234567890");
        assertThat(result.getPdfUrl()).contains("ValidarQR");
    }

    @Test
    void emitCreditNote_submitsAnulacionWhenRelatedInvoicePresent() {
        EmitCreditNoteCommand command = EmitCreditNoteCommand.builder()
                .emitterCuit("B12345678")
                .emitterRazonSocial("Solaris ES SL")
                .puntoVenta(1)
                .relatedInvoiceNumero(42L)
                .relatedFechaExpedicion("07-07-2026")
                .build();

        when(hashChainService.resolvePreviousHash(7L)).thenReturn("PREV");
        when(aeatClient.submitAnulacion(any(), any(), eq("B12345678"), eq("Solaris ES SL"), eq("PREV")))
                .thenReturn(VerifactuInvoiceAuthorization.authorized(
                        TipoComprobante.FACTURA_B,
                        1,
                        42L,
                        "ANULACIONHASH",
                        null,
                        "<request/>",
                        "<response/>"
                ));

        EmitInvoiceResult result = provider.emitCreditNote(command, VerifactuCredentials.empty(), 7L);

        assertThat(result.isAuthorized()).isTrue();
        assertThat(result.getCae()).isEqualTo("ANULACIONHASH");
    }

    @Test
    void emitCreditNote_rejectsWhenRelatedInvoiceMissing() {
        EmitCreditNoteCommand command = EmitCreditNoteCommand.builder()
                .emitterCuit("B12345678")
                .emitterRazonSocial("Solaris ES SL")
                .build();

        EmitInvoiceResult result = provider.emitCreditNote(command, VerifactuCredentials.empty(), 1L);

        assertThat(result.isAuthorized()).isFalse();
        assertThat(result.getRejectionReason()).contains("Related invoice");
    }

    @Test
    void emitInvoice_rejectsInvalidNif() {
        EmitInvoiceCommand command = EmitInvoiceCommand.builder()
                .emitterCuit("INVALID")
                .emitterRazonSocial("Solaris ES SL")
                .puntoVenta(1)
                .tipoComprobante(TipoComprobante.FACTURA_B)
                .numeroComprobante(1L)
                .importeTotal(new BigDecimal("10.00"))
                .build();

        EmitInvoiceResult result = provider.emitInvoice(command, VerifactuCredentials.empty(), 1L);

        assertThat(result.isAuthorized()).isFalse();
        assertThat(result.getRejectionReason()).contains("NIF/CIF");
    }
}
