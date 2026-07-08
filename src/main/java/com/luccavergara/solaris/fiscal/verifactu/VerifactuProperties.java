package com.luccavergara.solaris.fiscal.verifactu;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "verifactu")
public class VerifactuProperties {

    private Sandbox sandbox = new Sandbox();
    private Production production = new Production();
    private String nif = "";
    private Integer serie = 1;
    private Cert cert = new Cert();
    private Software software = new Software();
    private ResponsibleDeclaration responsibleDeclaration = new ResponsibleDeclaration();
    private Signature signature = new Signature();
    private boolean integrationTestEnabled = false;

    @Getter
    @Setter
    public static class Signature {
        private boolean enabled = true;
    }

    @Getter
    @Setter
    public static class Sandbox {
        private boolean enabled = false;
        private String wsdlUrl = VerifactuWsdlEndpoints.WSDL_SANDBOX;
        private String serviceUrl = VerifactuWsdlEndpoints.SANDBOX_VERIFACTU;
        private String serviceSelloUrl = VerifactuWsdlEndpoints.SANDBOX_VERIFACTU_SELLO;
        private String qrValidationBaseUrl = VerifactuWsdlEndpoints.SANDBOX_QR_VALIDATION;
    }

    @Getter
    @Setter
    public static class Production {
        private boolean enabled = false;
        private String wsdlUrl = VerifactuWsdlEndpoints.WSDL_PRODUCTION;
        private String serviceUrl = VerifactuWsdlEndpoints.PRODUCTION_VERIFACTU;
        private String serviceSelloUrl = VerifactuWsdlEndpoints.PRODUCTION_VERIFACTU_SELLO;
        private String qrValidationBaseUrl = VerifactuWsdlEndpoints.PRODUCTION_QR_VALIDATION;
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
        private String vendorNif = "";
        private String vendorName = "Solaris";
    }

    @Getter
    @Setter
    public static class ResponsibleDeclaration {
        /** Optional URL to the signed responsible declaration document. */
        private String url = "";
        /** Optional override for the declaration text shown to users. */
        private String text = "";
    }
}
