package com.luccavergara.solaris.fiscal.verifactu;

/**
 * Official AEAT Verifactu SOAP endpoints extracted from {@code SistemaFacturacion.wsdl} (tike v1.0).
 *
 * @see <a href="https://prewww2.aeat.es/static_files/common/internet/dep/aplicaciones/es/aeat/tikeV1.0/cont/ws/SistemaFacturacion.wsdl">Sandbox WSDL</a>
 */
public final class VerifactuWsdlEndpoints {

    public static final String WSDL_SANDBOX =
            "https://prewww2.aeat.es/static_files/common/internet/dep/aplicaciones/es/aeat/tikeV1.0/cont/ws/SistemaFacturacion.wsdl";
    public static final String WSDL_PRODUCTION =
            "https://www2.agenciatributaria.gob.es/static_files/common/internet/dep/aplicaciones/es/aeat/tikeV1.0/cont/ws/SistemaFacturacion.wsdl";

    /** Port {@code SistemaVerifactuPruebas} — client PKCS12 certificate. */
    public static final String SANDBOX_VERIFACTU =
            "https://prewww1.aeat.es/wlpl/TIKE-CONT/ws/SistemaFacturacion/VerifactuSOAP";
    /** Port {@code SistemaVerifactuSelloPruebas} — seal certificate. */
    public static final String SANDBOX_VERIFACTU_SELLO =
            "https://prewww10.aeat.es/wlpl/TIKE-CONT/ws/SistemaFacturacion/VerifactuSOAP";

    /** Port {@code SistemaVerifactu} — client PKCS12 certificate. */
    public static final String PRODUCTION_VERIFACTU =
            "https://www1.agenciatributaria.gob.es/wlpl/TIKE-CONT/ws/SistemaFacturacion/VerifactuSOAP";
    /** Port {@code SistemaVerifactuSello} — seal certificate. */
    public static final String PRODUCTION_VERIFACTU_SELLO =
            "https://www10.agenciatributaria.gob.es/wlpl/TIKE-CONT/ws/SistemaFacturacion/VerifactuSOAP";

    public static final String SANDBOX_QR_VALIDATION =
            "https://prewww1.aeat.es/wlpl/TIKE-CONT/ValidarQR";

    public static final String PRODUCTION_QR_VALIDATION =
            "https://www2.agenciatributaria.gob.es/wlpl/TIKE-CONT/ValidarQR";

    /** WSDL defines {@code soapAction=""} for all operations. */
    public static final String SOAP_ACTION = "";

    private VerifactuWsdlEndpoints() {
    }
}
