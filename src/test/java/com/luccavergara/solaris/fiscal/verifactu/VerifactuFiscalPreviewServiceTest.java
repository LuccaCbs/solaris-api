package com.luccavergara.solaris.fiscal.verifactu;

import com.luccavergara.solaris.entity.FiscalProviderType;
import com.luccavergara.solaris.entity.Organization;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VerifactuFiscalPreviewServiceTest {

    @Test
    void buildPreviewHtml_usesSandboxQrUrlByDefault() {
        VerifactuProperties properties = new VerifactuProperties();
        VerifactuEndpointResolver endpointResolver = new VerifactuEndpointResolver();
        VerifactuQrUrlBuilder qrUrlBuilder = new VerifactuQrUrlBuilder();
        VerifactuFiscalRepresentationBuilder representationBuilder = new VerifactuFiscalRepresentationBuilder();
        VerifactuSoftwareDeclarationService declarationService = new VerifactuSoftwareDeclarationService();
        VerifactuHashCalculator hashCalculator = new VerifactuHashCalculator();

        VerifactuFiscalPreviewService service = new VerifactuFiscalPreviewService(
                properties,
                endpointResolver,
                qrUrlBuilder,
                representationBuilder,
                declarationService,
                hashCalculator
        );

        Organization organization = Organization.builder()
                .cuit("B12345678")
                .razonSocial("Solaris ES SL")
                .fiscalPuntoVenta(1)
                .fiscalProvider(FiscalProviderType.VERIFACTU_NATIVE)
                .build();

        String html = service.buildPreviewHtml(organization);

        assertThat(html).contains("Representación fiscal Verifactu");
        assertThat(html).contains("B12345678");
        assertThat(html).contains("prewww1.aeat.es/wlpl/TIKE-CONT/ValidarQR");
        assertThat(html).contains("Declaración responsable del SIF");
    }
}
