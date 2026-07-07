package com.luccavergara.solaris.fiscal.verifactu;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VerifactuHashCalculatorTest {

    private final VerifactuHashCalculator calculator = new VerifactuHashCalculator();

    @Test
    void calculateAltaFingerprint_matchesAeatExampleShape() {
        VerifactuHashCalculator.VerifactuAltaRecord record = new VerifactuHashCalculator.VerifactuAltaRecord(
                "89890001K",
                "12345678/G33",
                "01-01-2024",
                "F1",
                "12.35",
                "123.45",
                "",
                "2024-01-01T19:20:30+01:00"
        );

        String huella = calculator.calculateAltaFingerprint(record);

        assertThat(huella).hasSize(64);
        assertThat(huella).isEqualTo("3C464DAF61ACB827C65FDA19F352A4E3BDC2C640E9E9FC4CC058073F38F12F60");
    }
}
