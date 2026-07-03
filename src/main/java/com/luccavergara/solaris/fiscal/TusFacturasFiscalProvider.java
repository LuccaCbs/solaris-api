package com.luccavergara.solaris.fiscal;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.luccavergara.solaris.entity.CondicionIva;
import com.luccavergara.solaris.entity.TipoComprobante;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Slf4j
@Component
@RequiredArgsConstructor
public class TusFacturasFiscalProvider implements FiscalProvider {

    private static final DateTimeFormatter TUSFACTURAS_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String DEFAULT_RUBRO = "Ventas";
    private static final String DEFAULT_PROVINCIA = "1";
    private static final String DEFAULT_DOMICILIO = "Sin domicilio";
    private static final String DEFAULT_CONDICION_PAGO = "201";

    private final ObjectMapper objectMapper;

    @Value("${application.fiscal.tusfacturas.base-url:https://www.tusfacturas.app/app/api/v2/facturacion/nuevo}")
    private String baseUrl;

    private RestClient restClient;

    @Override
    public EmitInvoiceResult emitInvoice(EmitInvoiceCommand command) {
        throw new IllegalStateException(
                "TusFacturas provider requires credentials; use emitInvoice(command, credentials)"
        );
    }

    public EmitInvoiceResult emitInvoice(EmitInvoiceCommand command, TusFacturasCredentials credentials) {
        Map<String, Object> payload = buildPayload(command, credentials);

        try {
            String responseBody = restClient().post()
                    .uri(baseUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(String.class);

            return parseResponse(command, responseBody);
        } catch (RestClientResponseException ex) {
            String responseBody = ex.getResponseBodyAsString();
            log.error(
                    "TusFacturas API HTTP {}: {}",
                    ex.getStatusCode().value(),
                    truncate(responseBody)
            );

            if (StringUtils.hasText(responseBody)) {
                return parseResponse(command, responseBody);
            }

            return rejected(command, "TusFacturas HTTP " + ex.getStatusCode().value() + ": " + ex.getMessage(), null);
        } catch (RestClientException ex) {
            log.error("TusFacturas API call failed: {}", ex.getMessage());
            return rejected(command, "TusFacturas request failed: " + ex.getMessage(), null);
        }
    }

    @Override
    public EmitInvoiceResult emitCreditNote(EmitCreditNoteCommand command) {
        throw new UnsupportedOperationException("Credit notes are not supported yet");
    }

    private RestClient restClient() {
        if (restClient == null) {
            SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
            requestFactory.setConnectTimeout(30_000);
            requestFactory.setReadTimeout(120_000);
            restClient = RestClient.builder().requestFactory(requestFactory).build();
        }
        return restClient;
    }

    private Map<String, Object> buildPayload(EmitInvoiceCommand command, TusFacturasCredentials credentials) {
        LocalDate today = LocalDate.now();
        boolean facturaB = command.getTipoComprobante() == TipoComprobante.FACTURA_B;

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("usertoken", credentials.userToken());
        payload.put("apitoken", credentials.apiToken());
        payload.put("apikey", credentials.apiKey());
        payload.put("cliente", buildCliente(command));
        payload.put("comprobante", buildComprobante(command, today, facturaB));
        return payload;
    }

    private Map<String, Object> buildCliente(EmitInvoiceCommand command) {
        Map<String, Object> cliente = new LinkedHashMap<>();
        cliente.put("documento_tipo", mapDocumentoTipo(command.getCustomerDocumentType()));
        cliente.put("documento_nro", command.getCustomerDocumentNumber());
        cliente.put("razon_social", command.getCustomerRazonSocial());
        cliente.put("domicilio", StringUtils.hasText(command.getCustomerAddress())
                ? command.getCustomerAddress()
                : DEFAULT_DOMICILIO);
        cliente.put("provincia", DEFAULT_PROVINCIA);
        cliente.put("envia_por_mail", "N");
        cliente.put("condicion_pago", DEFAULT_CONDICION_PAGO);
        String condicionIva = mapCondicionIva(command.getCustomerCondicionIva());
        cliente.put("condicion_iva", condicionIva);
        cliente.put("condicion_iva_operacion", condicionIva);
        cliente.put("codigo", buildClienteCodigo(command));
        cliente.put("rg5329", "N");
        cliente.put("reclama_deuda", "N");

        if (StringUtils.hasText(command.getCustomerEmail())) {
            cliente.put("email", command.getCustomerEmail());
        }

        return cliente;
    }

    private Map<String, Object> buildComprobante(EmitInvoiceCommand command, LocalDate today, boolean facturaB) {
        Map<String, Object> comprobante = new LinkedHashMap<>();
        comprobante.put("fecha", formatDate(today));
        comprobante.put("vencimiento", formatDate(today.plusDays(30)));
        comprobante.put("periodo_facturado_desde", formatDate(today));
        comprobante.put("periodo_facturado_hasta", formatDate(today));
        comprobante.put("tipo", mapTipoComprobante(command.getTipoComprobante()));
        comprobante.put("operacion", "V");
        comprobante.put("idioma", "1");
        comprobante.put("punto_venta", command.getPuntoVenta());
        comprobante.put("moneda", "PES");
        comprobante.put("cotizacion", 1);
        comprobante.put("numero", command.getNumeroComprobante());
        comprobante.put("rubro", DEFAULT_RUBRO);
        comprobante.put("rubro_grupo_contable", DEFAULT_RUBRO);
        comprobante.put("total", scaleMoney(command.getImporteTotal()));
        comprobante.put("detalle", buildDetalle(command, facturaB));
        comprobante.put("tributos", List.of());
        return comprobante;
    }

    private List<Map<String, Object>> buildDetalle(EmitInvoiceCommand command, boolean facturaB) {
        List<Map<String, Object>> detalle = new ArrayList<>();

        if (command.getLines() == null || command.getLines().isEmpty()) {
            detalle.add(buildFallbackLine(command, facturaB));
            return detalle;
        }

        int index = 1;
        for (EmitInvoiceCommand.InvoiceLineCommand line : command.getLines()) {
            detalle.add(buildDetalleLine(line, index++, facturaB));
        }

        return detalle;
    }

    private Map<String, Object> buildDetalleLine(
            EmitInvoiceCommand.InvoiceLineCommand line,
            int index,
            boolean facturaB
    ) {
        BigDecimal quantity = BigDecimal.valueOf(line.getQuantity() != null ? line.getQuantity() : 1);
        BigDecimal unitPriceWithoutIva = resolveUnitPriceWithoutIva(line, quantity, facturaB);
        BigDecimal alicuota = resolveAlicuota(line, facturaB);

        Map<String, Object> producto = new LinkedHashMap<>();
        producto.put("descripcion", line.getDescription());
        producto.put("codigo", "SOL-" + index);
        producto.put("lista_precios", "standard");
        producto.put("leyenda", "");
        producto.put("unidad_bulto", 1);
        producto.put("alicuota", alicuota);
        producto.put("actualiza_precio", "N");
        producto.put("rg5329", "N");
        producto.put("precio_unitario_sin_iva", unitPriceWithoutIva);

        Map<String, Object> item = new LinkedHashMap<>();
        item.put("cantidad", quantity);
        item.put("afecta_stock", "N");
        item.put("actualiza_precio", "N");
        item.put("bonificacion_porcentaje", 0);
        item.put("producto", producto);
        return item;
    }

    private Map<String, Object> buildFallbackLine(EmitInvoiceCommand command, boolean facturaB) {
        BigDecimal total = command.getImporteTotal() != null ? command.getImporteTotal() : BigDecimal.ZERO;
        EmitInvoiceCommand.InvoiceLineCommand fallback = EmitInvoiceCommand.InvoiceLineCommand.builder()
                .description("Venta")
                .quantity(1)
                .netAmount(facturaB ? command.getImporteNeto() : total)
                .ivaRate(BigDecimal.ZERO)
                .build();
        return buildDetalleLine(fallback, 1, facturaB);
    }

    private BigDecimal resolveUnitPriceWithoutIva(
            EmitInvoiceCommand.InvoiceLineCommand line,
            BigDecimal quantity,
            boolean facturaB
    ) {
        BigDecimal netAmount = line.getNetAmount();
        if (netAmount == null) {
            netAmount = line.getSubtotal() != null ? line.getSubtotal() : BigDecimal.ZERO;
        }

        if (quantity.signum() == 0) {
            return scaleMoney(netAmount);
        }

        return scaleMoney(netAmount.divide(quantity, 3, RoundingMode.HALF_UP));
    }

    private BigDecimal resolveAlicuota(EmitInvoiceCommand.InvoiceLineCommand line, boolean facturaB) {
        if (!facturaB) {
            return BigDecimal.ZERO;
        }

        if (line.getIvaRate() == null || line.getIvaRate().signum() == 0) {
            return BigDecimal.ZERO;
        }

        return line.getIvaRate()
                .multiply(BigDecimal.valueOf(100))
                .setScale(1, RoundingMode.HALF_UP)
                .stripTrailingZeros();
    }

    private EmitInvoiceResult parseResponse(EmitInvoiceCommand command, String responseBody) {
        if (!StringUtils.hasText(responseBody)) {
            return rejected(command, "Empty TusFacturas response", responseBody);
        }

        try {
            JsonNode root = objectMapper.readTree(responseBody);
            boolean hasError = "S".equalsIgnoreCase(root.path("error").asText(""));

            if (hasError) {
                return rejected(command, extractErrors(root), responseBody);
            }

            String cae = trimValue(root.path("cae").asText(null));
            LocalDate caeVencimiento = parseDate(trimValue(root.path("vencimiento_cae").asText(null)));
            String pdfUrl = trimValue(root.path("comprobante_pdf_url").asText(null));
            Long numeroComprobante = parseNumeroComprobante(root, command.getNumeroComprobante());

            return EmitInvoiceResult.builder()
                    .tipoComprobante(command.getTipoComprobante())
                    .puntoVenta(command.getPuntoVenta())
                    .numeroComprobante(numeroComprobante)
                    .cae(StringUtils.hasText(cae) ? cae : null)
                    .caeVencimiento(caeVencimiento)
                    .pdfUrl(pdfUrl)
                    .rawJson(responseBody)
                    .authorized(true)
                    .build();
        } catch (Exception ex) {
            log.error("Failed to parse TusFacturas response: {}", ex.getMessage());
            return rejected(command, "Invalid TusFacturas response", responseBody);
        }
    }

    private EmitInvoiceResult rejected(EmitInvoiceCommand command, String reason, String rawJson) {
        return EmitInvoiceResult.builder()
                .tipoComprobante(command.getTipoComprobante())
                .puntoVenta(command.getPuntoVenta())
                .numeroComprobante(command.getNumeroComprobante())
                .authorized(false)
                .rejectionReason(reason)
                .rawJson(rawJson)
                .build();
    }

    private String extractErrors(JsonNode root) {
        JsonNode errores = root.path("errores");
        if (errores.isArray() && !errores.isEmpty()) {
            String joined = StreamSupport.stream(errores.spliterator(), false)
                    .map(JsonNode::asText)
                    .filter(StringUtils::hasText)
                    .collect(Collectors.joining("; "));
            if (StringUtils.hasText(joined)) {
                return joined;
            }
        }

        JsonNode errorDetails = root.path("error_details");
        if (errorDetails.isArray() && !errorDetails.isEmpty()) {
            String joined = StreamSupport.stream(errorDetails.spliterator(), false)
                    .map(node -> node.path("text").asText(""))
                    .filter(StringUtils::hasText)
                    .collect(Collectors.joining("; "));
            if (StringUtils.hasText(joined)) {
                return joined;
            }
        }

        return "TusFacturas rejected the invoice";
    }

    private Long parseNumeroComprobante(JsonNode root, Long fallback) {
        String raw = trimValue(root.path("comprobante_nro").asText(null));
        if (!StringUtils.hasText(raw)) {
            return fallback;
        }

        String digits = raw.replaceAll("\\D", "");
        if (!StringUtils.hasText(digits)) {
            return fallback;
        }

        try {
            return Long.parseLong(digits);
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private String mapTipoComprobante(TipoComprobante tipoComprobante) {
        return switch (tipoComprobante) {
            case FACTURA_B -> "FACTURA B";
            case FACTURA_C -> "FACTURA C";
        };
    }

    private String mapDocumentoTipo(String documentType) {
        if (!StringUtils.hasText(documentType)) {
            return "DNI";
        }

        return switch (documentType.toUpperCase(Locale.ROOT)) {
            case "CUIT", "CUIL" -> "CUIT";
            case "PASAPORTE" -> "PASAPORTE";
            case "OTRO" -> "OTRO";
            default -> "DNI";
        };
    }

    private String mapCondicionIva(CondicionIva condicionIva) {
        if (condicionIva == null) {
            return "CF";
        }

        return switch (condicionIva) {
            case RESPONSABLE_INSCRIPTO -> "RI";
            case MONOTRIBUTO -> "M";
            case EXENTO -> "E";
            case CONSUMIDOR_FINAL, NO_CATEGORIZADO -> "CF";
        };
    }

    private String buildClienteCodigo(EmitInvoiceCommand command) {
        return mapDocumentoTipo(command.getCustomerDocumentType())
                + "-"
                + command.getCustomerDocumentNumber();
    }

    private String formatDate(LocalDate date) {
        return TUSFACTURAS_DATE.format(date);
    }

    private LocalDate parseDate(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        try {
            return LocalDate.parse(value.trim(), TUSFACTURAS_DATE);
        } catch (DateTimeParseException ex) {
            try {
                return LocalDate.parse(value.trim());
            } catch (DateTimeParseException ignored) {
                return null;
            }
        }
    }

    private BigDecimal scaleMoney(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private String trimValue(String value) {
        return value != null ? value.trim() : null;
    }

    private String truncate(String value) {
        if (value == null) {
            return "";
        }
        return value.length() <= 500 ? value : value.substring(0, 500) + "...";
    }
}
