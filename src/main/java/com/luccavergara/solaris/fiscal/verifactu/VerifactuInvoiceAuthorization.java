package com.luccavergara.solaris.fiscal.verifactu;

import com.luccavergara.solaris.entity.TipoComprobante;
import lombok.Getter;

@Getter
public class VerifactuInvoiceAuthorization {

    private final TipoComprobante tipoComprobante;
    private final Integer puntoVenta;
    private final Long numeroComprobante;
    private final String huella;
    private final String qrUrl;
    private final String requestXml;
    private final String responseXml;
    private final boolean authorized;
    private final String rejectionReason;

    private VerifactuInvoiceAuthorization(
            TipoComprobante tipoComprobante,
            Integer puntoVenta,
            Long numeroComprobante,
            String huella,
            String qrUrl,
            String requestXml,
            String responseXml,
            boolean authorized,
            String rejectionReason
    ) {
        this.tipoComprobante = tipoComprobante;
        this.puntoVenta = puntoVenta;
        this.numeroComprobante = numeroComprobante;
        this.huella = huella;
        this.qrUrl = qrUrl;
        this.requestXml = requestXml;
        this.responseXml = responseXml;
        this.authorized = authorized;
        this.rejectionReason = rejectionReason;
    }

    public static VerifactuInvoiceAuthorization authorized(
            TipoComprobante tipoComprobante,
            Integer puntoVenta,
            Long numeroComprobante,
            String huella,
            String qrUrl,
            String requestXml,
            String responseXml
    ) {
        return new VerifactuInvoiceAuthorization(
                tipoComprobante,
                puntoVenta,
                numeroComprobante,
                huella,
                qrUrl,
                requestXml,
                responseXml,
                true,
                null
        );
    }

    public static VerifactuInvoiceAuthorization rejected(
            TipoComprobante tipoComprobante,
            Integer puntoVenta,
            Long numeroComprobante,
            String huella,
            String qrUrl,
            String requestXml,
            String rejectionReason
    ) {
        return new VerifactuInvoiceAuthorization(
                tipoComprobante,
                puntoVenta,
                numeroComprobante,
                huella,
                qrUrl,
                requestXml,
                null,
                false,
                rejectionReason
        );
    }
}
