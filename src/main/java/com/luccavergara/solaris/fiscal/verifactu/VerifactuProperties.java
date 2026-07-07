package com.luccavergara.solaris.fiscal.verifactu;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "verifactu")
public class VerifactuProperties {

    private Sandbox sandbox = new Sandbox();
    private String nif = "";
    private Integer serie = 1;
    private Cert cert = new Cert();
    private Software software = new Software();
    private boolean integrationTestEnabled = false;

    @Getter
    @Setter
    public static class Sandbox {
        private boolean enabled = false;
        private String wsdlUrl =
                "https://prewww2.aeat.es/static_files/common/internet/dep/aplicaciones/es/aeat/tikeV1.0/cont/ws/SistemaFacturacion.wsdl";
        private String serviceUrl =
                "https://prewww2.aeat.es/wlpl/TIKE-CONT/ws/SistemaFacturacion/VerifactuSOAP";
        private String qrValidationBaseUrl =
                "https://prewww2.aeat.es/wlpl/TIKE-CONT/ValidarQR";
    }

    @Getter
    @Setter
    public static class Cert {
        private String path = "";
        private String password = "";
        private String base64 = "";
    }

    @Getter
    @Setter
    public static class Software {
        private String name = "Solaris Manager";
        private String id = "SO";
        private String version = "1.0";
        private String installationNumber = "1";
    }
}
