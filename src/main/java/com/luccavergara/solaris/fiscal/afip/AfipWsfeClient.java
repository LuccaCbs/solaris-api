package com.luccavergara.solaris.fiscal.afip;

import com.luccavergara.solaris.entity.TipoComprobante;
import com.luccavergara.solaris.fiscal.EmitInvoiceCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Slf4j
@Component
@RequiredArgsConstructor
public class AfipWsfeClient {

    private static final DateTimeFormatter AFIP_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String WSFE_NAMESPACE = "http://ar.gov.afip.dif.FEV1/";

    private final AfipProperties afipProperties;
    private final AfipSoapTransport soapTransport;
    private final AfipWsaaClient wsaaClient;

    public AfipInvoiceAuthorization authorizeInvoice(
            EmitInvoiceCommand command,
            AfipCredentials credentials,
            String cuit
    ) {
        if (command.getTipoComprobante() != TipoComprobante.FACTURA_C) {
            throw new UnsupportedOperationException(
                    "AFIP native POC supports Factura C (tipo 11) only in homologation"
            );
        }

        AfipAuthToken auth = wsaaClient.authenticate(credentials, cuit);
        int puntoVenta = command.getPuntoVenta();
        int cbteTipo = command.getTipoComprobante().getAfipCode();

        long ultimoAutorizado = fetchUltimoAutorizado(auth, cuit, puntoVenta, cbteTipo);
        long numeroComprobante = resolveNumeroComprobante(command.getNumeroComprobante(), ultimoAutorizado);

        String response = requestCae(auth, cuit, command, numeroComprobante);
        return parseAuthorizationResponse(response, command, numeroComprobante);
    }

    long fetchUltimoAutorizado(AfipAuthToken auth, String cuit, int puntoVenta, int cbteTipo) {
        String envelope = """
                <?xml version="1.0" encoding="utf-8"?>
                <soap:Envelope xmlns:soap="http://www.w3.org/2003/05/soap-envelope" xmlns:ar="%s">
                  <soap:Header/>
                  <soap:Body>
                    <ar:FECompUltimoAutorizado>
                      <ar:Auth>
                        <ar:Token>%s</ar:Token>
                        <ar:Sign>%s</ar:Sign>
                        <ar:Cuit>%s</ar:Cuit>
                      </ar:Auth>
                      <ar:PtoVta>%d</ar:PtoVta>
                      <ar:CbteTipo>%d</ar:CbteTipo>
                    </ar:FECompUltimoAutorizado>
                  </soap:Body>
                </soap:Envelope>
                """.formatted(
                WSFE_NAMESPACE,
                escapeXml(auth.token()),
                escapeXml(auth.sign()),
                cuit,
                puntoVenta,
                cbteTipo
        );

        String response = soapTransport.post(
                afipProperties.getHomologation().getWsfeUrl(),
                WSFE_NAMESPACE + "FECompUltimoAutorizado",
                envelope
        );

        String fault = AfipXmlHelper.firstFault(response);
        if (StringUtils.hasText(fault)) {
            throw new IllegalStateException("FECompUltimoAutorizado failed: " + fault);
        }

        String cbteNro = AfipXmlHelper.textContent(response, "CbteNro");
        if (!StringUtils.hasText(cbteNro)) {
            return 0L;
        }

        return Long.parseLong(cbteNro.trim());
    }

    private long resolveNumeroComprobante(Long requested, long ultimoAutorizado) {
        long expected = ultimoAutorizado + 1;
        if (requested == null || requested <= 0) {
            return expected;
        }

        if (!requested.equals(expected)) {
            log.warn(
                    "AFIP expected invoice number {} but command requested {} — using AFIP sequence",
                    expected,
                    requested
            );
        }

        return expected;
    }

    private String requestCae(
            AfipAuthToken auth,
            String cuit,
            EmitInvoiceCommand command,
            long numeroComprobante
    ) {
        LocalDate today = LocalDate.now();
        String fecha = AFIP_DATE.format(today);
        BigDecimal importeTotal = scaleMoney(command.getImporteTotal());
        DocumentMapping doc = mapDocument(command);

        String envelope = """
                <?xml version="1.0" encoding="utf-8"?>
                <soap:Envelope xmlns:soap="http://www.w3.org/2003/05/soap-envelope" xmlns:ar="%s">
                  <soap:Header/>
                  <soap:Body>
                    <ar:FECAESolicitar>
                      <ar:Auth>
                        <ar:Token>%s</ar:Token>
                        <ar:Sign>%s</ar:Sign>
                        <ar:Cuit>%s</ar:Cuit>
                      </ar:Auth>
                      <ar:FeCAEReq>
                        <ar:FeCabReq>
                          <ar:CantReg>1</ar:CantReg>
                          <ar:PtoVta>%d</ar:PtoVta>
                          <ar:CbteTipo>%d</ar:CbteTipo>
                        </ar:FeCabReq>
                        <ar:FeDetReq>
                          <ar:FECAEDetRequest>
                            <ar:Concepto>1</ar:Concepto>
                            <ar:DocTipo>%d</ar:DocTipo>
                            <ar:DocNro>%d</ar:DocNro>
                            <ar:CbteDesde>%d</ar:CbteDesde>
                            <ar:CbteHasta>%d</ar:CbteHasta>
                            <ar:CbteFch>%s</ar:CbteFch>
                            <ar:ImpTotal>%s</ar:ImpTotal>
                            <ar:ImpTotConc>0.00</ar:ImpTotConc>
                            <ar:ImpNeto>%s</ar:ImpNeto>
                            <ar:ImpOpEx>0.00</ar:ImpOpEx>
                            <ar:ImpIVA>0.00</ar:ImpIVA>
                            <ar:ImpTrib>0.00</ar:ImpTrib>
                            <ar:MonId>PES</ar:MonId>
                            <ar:MonCotiz>1</ar:MonCotiz>
                          </ar:FECAEDetRequest>
                        </ar:FeDetReq>
                      </ar:FeCAEReq>
                    </ar:FECAESolicitar>
                  </soap:Body>
                </soap:Envelope>
                """.formatted(
                WSFE_NAMESPACE,
                escapeXml(auth.token()),
                escapeXml(auth.sign()),
                cuit,
                command.getPuntoVenta(),
                command.getTipoComprobante().getAfipCode(),
                doc.docTipo(),
                doc.docNro(),
                numeroComprobante,
                numeroComprobante,
                fecha,
                importeTotal,
                importeTotal
        );

        return soapTransport.post(
                afipProperties.getHomologation().getWsfeUrl(),
                WSFE_NAMESPACE + "FECAESolicitar",
                envelope
        );
    }

    AfipInvoiceAuthorization parseAuthorizationResponse(
            String response,
            EmitInvoiceCommand command,
            long numeroComprobante
    ) {
        String fault = AfipXmlHelper.firstFault(response);
        if (StringUtils.hasText(fault)) {
            return AfipInvoiceAuthorization.rejected(response, fault);
        }

        String resultado = AfipXmlHelper.textContent(response, "Resultado");
        if (!"A".equalsIgnoreCase(resultado)) {
            String observations = AfipXmlHelper.textContent(response, "Msg");
            String code = AfipXmlHelper.textContent(response, "Code");
            String reason = StringUtils.hasText(observations)
                    ? observations
                    : "AFIP rejected invoice (Resultado=" + resultado + ")";
            if (StringUtils.hasText(code)) {
                reason = code + ": " + reason;
            }
            return AfipInvoiceAuthorization.rejected(response, reason);
        }

        String cae = AfipXmlHelper.textContent(response, "CAE");
        String caeVto = AfipXmlHelper.textContent(response, "CAEFchVto");

        return AfipInvoiceAuthorization.authorized(
                command.getTipoComprobante(),
                command.getPuntoVenta(),
                numeroComprobante,
                cae,
                AfipXmlHelper.parseAfipDate(caeVto),
                response
        );
    }

    private DocumentMapping mapDocument(EmitInvoiceCommand command) {
        String documentType = command.getCustomerDocumentType();
        String documentNumber = command.getCustomerDocumentNumber();

        if (!StringUtils.hasText(documentType)) {
            return new DocumentMapping(99, 0L);
        }

        return switch (documentType.toUpperCase(Locale.ROOT)) {
            case "CUIT", "CUIL" -> new DocumentMapping(
                    80,
                    parseDocumentNumber(documentNumber)
            );
            case "DNI" -> new DocumentMapping(96, parseDocumentNumber(documentNumber));
            case "PASAPORTE" -> new DocumentMapping(94, parseDocumentNumber(documentNumber));
            default -> {
                long docNro = parseDocumentNumber(documentNumber);
                if (docNro == 0L) {
                    yield new DocumentMapping(99, 0L);
                }
                yield new DocumentMapping(96, docNro);
            }
        };
    }

    private long parseDocumentNumber(String documentNumber) {
        if (!StringUtils.hasText(documentNumber)) {
            return 0L;
        }

        String digits = documentNumber.replaceAll("\\D", "");
        if (!StringUtils.hasText(digits)) {
            return 0L;
        }

        try {
            return Long.parseLong(digits);
        } catch (NumberFormatException ex) {
            return 0L;
        }
    }

    private BigDecimal scaleMoney(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private String escapeXml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private record DocumentMapping(int docTipo, long docNro) {
    }
}
