package com.luccavergara.solaris.fiscal.verifactu;

import com.luccavergara.solaris.fiscal.EmitInvoiceCommand;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Component
public class VerifactuXmlBuilder {

    private static final String NAMESPACE =
            "https://www2.agenciatributaria.gob.es/static_files/common/internet/dep/aplicaciones/es/aeat/tike/cont/ws/SuministroLR.xsd";
    private static final DateTimeFormatter EXPEDITION_DATE = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    public String buildRegFactuEnvelope(VerifactuSubmission submission) {
        EmitInvoiceCommand command = submission.command();
        String destinatarios = buildDestinatarios(command);
        String desglose = buildDesglose(command);

        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:sfLR="%s">
                  <soapenv:Header/>
                  <soapenv:Body>
                    <sfLR:RegFactuSistemaFacturacion>
                      <sfLR:Cabecera>
                        <sfLR:ObligadoEmision>
                          <sfLR:NombreRazon>%s</sfLR:NombreRazon>
                          <sfLR:NIF>%s</sfLR:NIF>
                        </sfLR:ObligadoEmision>
                      </sfLR:Cabecera>
                      <sfLR:RegistroFactura>
                        <sfLR:RegistroAlta>
                          <sfLR:IDVersion>1.0</sfLR:IDVersion>
                          <sfLR:IDFactura>
                            <sfLR:IDEmisorFactura>%s</sfLR:IDEmisorFactura>
                            <sfLR:NumSerieFactura>%s</sfLR:NumSerieFactura>
                            <sfLR:FechaExpedicionFactura>%s</sfLR:FechaExpedicionFactura>
                          </sfLR:IDFactura>
                          <sfLR:NombreRazonEmisor>%s</sfLR:NombreRazonEmisor>
                          <sfLR:TipoFactura>%s</sfLR:TipoFactura>
                          <sfLR:DescripcionOperacion>%s</sfLR:DescripcionOperacion>
                          %s
                          <sfLR:Desglose>%s</sfLR:Desglose>
                          <sfLR:CuotaTotal>%s</sfLR:CuotaTotal>
                          <sfLR:ImporteTotal>%s</sfLR:ImporteTotal>
                          <sfLR:SistemaInformatico>
                            <sfLR:NombreRazon>%s</sfLR:NombreRazon>
                            <sfLR:NIF>%s</sfLR:NIF>
                            <sfLR:NombreSistemaInformatico>%s</sfLR:NombreSistemaInformatico>
                            <sfLR:IdSistemaInformatico>%s</sfLR:IdSistemaInformatico>
                            <sfLR:Version>%s</sfLR:Version>
                            <sfLR:NumeroInstalacion>%s</sfLR:NumeroInstalacion>
                            <sfLR:TipoUsoPosibleSoloVerifactu>S</sfLR:TipoUsoPosibleSoloVerifactu>
                            <sfLR:TipoUsoPosibleMultiOT>N</sfLR:TipoUsoPosibleMultiOT>
                            <sfLR:IndicadorMultiplesOT>N</sfLR:IndicadorMultiplesOT>
                          </sfLR:SistemaInformatico>
                          <sfLR:TipoHuella>01</sfLR:TipoHuella>
                          <sfLR:Huella>%s</sfLR:Huella>
                          <sfLR:FechaHoraHusoGenRegistro>%s</sfLR:FechaHoraHusoGenRegistro>
                        </sfLR:RegistroAlta>
                      </sfLR:RegistroFactura>
                    </sfLR:RegFactuSistemaFacturacion>
                  </soapenv:Body>
                </soapenv:Envelope>
                """.formatted(
                NAMESPACE,
                VerifactuXmlHelper.escapeXml(submission.emitterRazonSocial()),
                VerifactuXmlHelper.escapeXml(submission.nif()),
                VerifactuXmlHelper.escapeXml(submission.nif()),
                VerifactuXmlHelper.escapeXml(submission.numSerieFactura()),
                EXPEDITION_DATE.format(submission.fechaExpedicion()),
                VerifactuXmlHelper.escapeXml(submission.emitterRazonSocial()),
                VerifactuXmlHelper.escapeXml(submission.tipoFactura()),
                VerifactuXmlHelper.escapeXml(submission.descripcionOperacion()),
                destinatarios,
                desglose,
                formatAmount(command.getImporteIva()),
                formatAmount(command.getImporteTotal()),
                VerifactuXmlHelper.escapeXml(submission.softwareVendorName()),
                VerifactuXmlHelper.escapeXml(submission.nif()),
                VerifactuXmlHelper.escapeXml(submission.softwareName()),
                VerifactuXmlHelper.escapeXml(submission.softwareId()),
                VerifactuXmlHelper.escapeXml(submission.softwareVersion()),
                VerifactuXmlHelper.escapeXml(submission.installationNumber()),
                VerifactuXmlHelper.escapeXml(submission.huella()),
                submission.fechaHoraHusoGenRegistro()
        );
    }

    private String buildDestinatarios(EmitInvoiceCommand command) {
        if (!hasCustomer(command)) {
            return "";
        }

        return """
                          <sfLR:Destinatarios>
                            <sfLR:IDDestinatario>
                              <sfLR:NombreRazon>%s</sfLR:NombreRazon>
                              <sfLR:NIF>%s</sfLR:NIF>
                            </sfLR:IDDestinatario>
                          </sfLR:Destinatarios>
                """.formatted(
                VerifactuXmlHelper.escapeXml(command.getCustomerRazonSocial()),
                VerifactuXmlHelper.escapeXml(command.getCustomerDocumentNumber())
        );
    }

    private String buildDesglose(EmitInvoiceCommand command) {
        return """
                            <sfLR:DetalleDesglose>
                              <sfLR:Impuesto>01</sfLR:Impuesto>
                              <sfLR:ClaveRegimen>01</sfLR:ClaveRegimen>
                              <sfLR:CalificacionOperacion>S1</sfLR:CalificacionOperacion>
                              <sfLR:TipoImpositivo>21</sfLR:TipoImpositivo>
                              <sfLR:BaseImponibleOimporteNoSujeto>%s</sfLR:BaseImponibleOimporteNoSujeto>
                              <sfLR:CuotaRepercutida>%s</sfLR:CuotaRepercutida>
                            </sfLR:DetalleDesglose>
                """.formatted(
                formatAmount(command.getImporteNeto()),
                formatAmount(command.getImporteIva())
        );
    }

    private boolean hasCustomer(EmitInvoiceCommand command) {
        return command.getCustomerDocumentNumber() != null
                && !"0".equals(command.getCustomerDocumentNumber())
                && command.getCustomerRazonSocial() != null;
    }

    private String formatAmount(BigDecimal amount) {
        if (amount == null) {
            return "0";
        }
        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    public record VerifactuSubmission(
            EmitInvoiceCommand command,
            String nif,
            String emitterRazonSocial,
            String numSerieFactura,
            LocalDate fechaExpedicion,
            String tipoFactura,
            String descripcionOperacion,
            String huella,
            String fechaHoraHusoGenRegistro,
            String softwareVendorName,
            String softwareName,
            String softwareId,
            String softwareVersion,
            String installationNumber
    ) {
        public static String nowIsoSpain() {
            return OffsetDateTime.now(ZoneOffset.ofHours(1)).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        }
    }
}
