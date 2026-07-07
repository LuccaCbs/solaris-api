package com.luccavergara.solaris.fiscal.afip;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "afip")
public class AfipProperties {

    private Homologation homologation = new Homologation();
    private String cuit;
    private Integer puntoVenta = 1;
    private Cert cert = new Cert();
    private IntegrationTest integrationTest = new IntegrationTest();

    @Getter
    @Setter
    public static class Homologation {
        private boolean enabled = false;
        private String wsaaUrl = "https://wsaahomo.afip.gov.ar/ws/services/LoginCms";
        private String wsfeUrl = "https://wswhomo.afip.gov.ar/wsfev1/service.asmx";
    }

    @Getter
    @Setter
    public static class Cert {
        private String path;
        private String password;
        private String base64;
    }

    @Getter
    @Setter
    public static class IntegrationTest {
        private boolean enabled = false;
    }
}
