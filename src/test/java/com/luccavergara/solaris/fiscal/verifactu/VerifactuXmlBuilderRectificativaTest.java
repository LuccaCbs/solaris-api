package com.luccavergara.solaris.fiscal.verifactu;

import com.luccavergara.solaris.entity.TipoComprobante;
import com.luccavergara.solaris.fiscal.EmitInvoiceCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class VerifactuXmlBuilderRectificativaTest {

    private VerifactuXmlBuilder xmlBuilder;

    @BeforeEach
    void setUp() {
        xmlBuilder = new VerifactuXmlBuilder();
    }

    @Test
    void buildRegistroAltaFragment_includesRectificativaFieldsForR1() {
        EmitInvoiceCommand command = EmitInvoiceCommand.builder()
                .importeNeto(new BigDecimal("-100.00"))
                .importeIva(new BigDecimal("-21.00"))
                .importeTotal(new BigDecimal("-121.00"))
                .build();

        VerifactuXmlBuilder.VerifactuSubmission submission = VerifactuXmlBuilder.VerifactuSubmission.forRectificativa(
                command,
                "B12345678",
                "Solaris ES SL",
                "1-11",
                LocalDate.of(2026, 7, 8),
                "R1",
                "I",
                "1-10",
                "07-07-2026",
                null,
                null,
                "Rectificativa por diferencias",
                "HASH123",
                "2026-07-08T12:00:00+01:00",
                "Solaris ES SL",
                "Solaris Manager",
                "SO",
                "1.0",
                "1"
        );

        String xml = xmlBuilder.buildRegistroAltaFragment(submission);

        assertThat(xml).contains("<sfLR:TipoFactura>R1</sfLR:TipoFactura>");
        assertThat(xml).contains("<sfLR:TipoRectificativa>I</sfLR:TipoRectificativa>");
        assertThat(xml).contains("<sfLR:NumSerieFactura>1-10</sfLR:NumSerieFactura>");
        assertThat(xml).contains("<sfLR:FechaExpedicionFactura>07-07-2026</sfLR:FechaExpedicionFactura>");
        assertThat(xml).doesNotContain("ImporteRectificacion");
    }

    @Test
    void buildRegistroAltaFragment_includesImporteRectificacionForSubstitution() {
        EmitInvoiceCommand command = EmitInvoiceCommand.builder()
                .importeNeto(new BigDecimal("100.00"))
                .importeIva(new BigDecimal("21.00"))
                .importeTotal(new BigDecimal("121.00"))
                .build();

        VerifactuXmlBuilder.VerifactuSubmission submission = VerifactuXmlBuilder.VerifactuSubmission.forRectificativa(
                command,
                "B12345678",
                "Solaris ES SL",
                "1-12",
                LocalDate.of(2026, 7, 8),
                "R2",
                "S",
                "1-10",
                "07-07-2026",
                new BigDecimal("100.00"),
                new BigDecimal("21.00"),
                "Rectificativa por sustitución",
                "HASH456",
                "2026-07-08T12:00:00+01:00",
                "Solaris ES SL",
                "Solaris Manager",
                "SO",
                "1.0",
                "1"
        );

        String xml = xmlBuilder.buildRegistroAltaFragment(submission);

        assertThat(xml).contains("<sfLR:TipoFactura>R2</sfLR:TipoFactura>");
        assertThat(xml).contains("<sfLR:BaseRectificada>100.00</sfLR:BaseRectificada>");
        assertThat(xml).contains("<sfLR:CuotaRectificada>21.00</sfLR:CuotaRectificada>");
    }
}
