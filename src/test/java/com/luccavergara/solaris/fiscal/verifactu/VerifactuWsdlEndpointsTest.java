package com.luccavergara.solaris.fiscal.verifactu;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VerifactuWsdlEndpointsTest {

    @Test
    void sandboxVerifactuEndpoint_matchesOfficialWsdlPort() {
        assertThat(VerifactuWsdlEndpoints.SANDBOX_VERIFACTU)
                .isEqualTo("https://prewww1.aeat.es/wlpl/TIKE-CONT/ws/SistemaFacturacion/VerifactuSOAP");
    }

    @Test
    void productionVerifactuEndpoint_matchesOfficialWsdlPort() {
        assertThat(VerifactuWsdlEndpoints.PRODUCTION_VERIFACTU)
                .isEqualTo("https://www1.agenciatributaria.gob.es/wlpl/TIKE-CONT/ws/SistemaFacturacion/VerifactuSOAP");
    }

    @Test
    void productionQrEndpoint_matchesOfficialPattern() {
        assertThat(VerifactuWsdlEndpoints.PRODUCTION_QR_VALIDATION)
                .isEqualTo("https://www2.agenciatributaria.gob.es/wlpl/TIKE-CONT/ValidarQR");
    }

    @Test
    void soapAction_isEmptyPerWsdl() {
        assertThat(VerifactuWsdlEndpoints.SOAP_ACTION).isEmpty();
    }
}
