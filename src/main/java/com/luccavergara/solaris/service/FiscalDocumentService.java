package com.luccavergara.solaris.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.luccavergara.solaris.dto.EmitInvoiceRequest;
import com.luccavergara.solaris.dto.FiscalConfigRequest;
import com.luccavergara.solaris.dto.FiscalConfigResponse;
import com.luccavergara.solaris.dto.FiscalDocumentResponse;
import com.luccavergara.solaris.entity.*;
import com.luccavergara.solaris.exception.DuplicateResourceException;
import com.luccavergara.solaris.exception.ResourceNotFoundException;
import com.luccavergara.solaris.fiscal.*;
import com.luccavergara.solaris.repository.FiscalDocumentRepository;
import com.luccavergara.solaris.repository.OrganizationRepository;
import com.luccavergara.solaris.repository.StoreRepository;
import com.luccavergara.solaris.tenant.TenantContext;
import com.luccavergara.solaris.util.SpainTaxIdValidator;
import com.luccavergara.solaris.util.TaxIdNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FiscalDocumentService {

    private static final String CF_DOCUMENT_NUMBER = "0";
    private static final String CF_RAZON_SOCIAL = "Consumidor Final";
    private static final String ES_CF_RAZON_SOCIAL = "Cliente final";
    private static final long MAX_NUMERO_COMPROBANTE = 99_999_999L;

    private final FiscalDocumentRepository fiscalDocumentRepository;
    private final OrganizationRepository organizationRepository;
    private final StoreRepository storeRepository;
    private final TenantQueryService tenantQueryService;
    private final TenantScopeService tenantScopeService;
    private final FiscalProviderFactory fiscalProviderFactory;
    private final ObjectMapper objectMapper;
    private final EntitlementService entitlementService;

    @Transactional
    public FiscalDocumentResponse emitInvoiceForSale(Long saleId, EmitInvoiceRequest request) {
        assertFiscalModule();
        Sale sale = tenantQueryService.findSaleById(saleId)
                .orElseThrow(() -> new ResourceNotFoundException("Sale not found"));

        Organization organization = resolveOrganizationForInvoicing(
                sale,
                tenantScopeService.getCurrentUser()
        );

        fiscalDocumentRepository.findBySaleId(saleId)
                .ifPresent(existing -> {
                    throw new DuplicateResourceException("Sale is already invoiced");
                });

        validateFiscalConfig(organization);

        boolean spain = isSpainJurisdiction(organization);
        Integer puntoVenta = spain ? resolveSpainSeries(organization) : resolvePuntoVenta(organization);
        Store store = resolveStore(organization);
        Customer customer = resolveCustomer(request != null ? request.getCustomerId() : null);

        TipoComprobante tipoComprobante = spain
                ? TipoComprobante.FACTURA_B
                : FiscalInvoiceCalculator.resolveTipoComprobante(organization.getCondicionIva());
        boolean includesIvaBreakdown = spain || tipoComprobante == TipoComprobante.FACTURA_B;

        InvoiceTotals totals = spain
                ? calculateSpainTotals(sale)
                : calculateTotals(sale, includesIvaBreakdown);
        Long nextNumero = fiscalDocumentRepository.findMaxNumeroComprobante(
                organization.getId(),
                puntoVenta,
                tipoComprobante
        ) + 1;

        if (nextNumero > MAX_NUMERO_COMPROBANTE) {
            throw new IllegalStateException(
                    "Se alcanzó el número máximo de comprobante (8 dígitos) para este punto de venta"
            );
        }

        EmitInvoiceCommand command = buildCommand(
                organization,
                customer,
                tipoComprobante,
                puntoVenta,
                nextNumero,
                totals,
                sale,
                includesIvaBreakdown,
                spain
        );

        FiscalProvider provider = fiscalProviderFactory.resolve(organization);
        EmitInvoiceResult result = provider.emitInvoice(command);

        FiscalDocumentStatus status = result.isAuthorized()
                ? FiscalDocumentStatus.AUTHORIZED
                : FiscalDocumentStatus.REJECTED;

        FiscalDocument document = FiscalDocument.builder()
                .organization(organization)
                .store(store)
                .sale(sale)
                .customer(customer)
                .tipoComprobante(result.getTipoComprobante() != null ? result.getTipoComprobante() : tipoComprobante)
                .puntoVenta(result.getPuntoVenta() != null ? result.getPuntoVenta() : puntoVenta)
                .numeroComprobante(result.getNumeroComprobante() != null ? result.getNumeroComprobante() : nextNumero)
                .cae(result.getCae())
                .caeVencimiento(result.getCaeVencimiento())
                .importeNeto(totals.neto())
                .importeIva(totals.iva())
                .importeTotal(totals.total())
                .status(status)
                .afipRawJson(result.getRawJson())
                .rejectionReason(resolveRejectionReason(result))
                .pdfUrl(result.getPdfUrl())
                .build();

        FiscalDocument saved = fiscalDocumentRepository.save(document);
        return mapToResponse(saved);
    }

    public List<FiscalDocumentResponse> getAllFiscalDocuments() {
        assertFiscalModule();

        return tenantScopeService.resolveOrganizationId(tenantScopeService.getCurrentUser())
                .map(fiscalDocumentRepository::findAllByOrganizationIdOrderByCreatedAtDesc)
                .orElse(List.of())
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public FiscalDocumentResponse getFiscalDocumentById(Long id) {
        assertFiscalModule();

        return tenantScopeService.resolveOrganizationId(tenantScopeService.getCurrentUser())
                .flatMap(orgId -> fiscalDocumentRepository.findByIdAndOrganizationId(id, orgId))
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Fiscal document not found"));
    }

    public List<FiscalDocumentResponse> getFiscalDocumentsByCustomerId(Long customerId) {
        assertFiscalModule();

        tenantQueryService.findCustomerById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        return fiscalDocumentRepository.findAllByCustomerIdOrderByCreatedAtDesc(customerId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public FiscalDocumentResponse getFiscalDocumentBySaleId(Long saleId) {
        assertFiscalModule();

        return tenantScopeService.resolveOrganizationId(tenantScopeService.getCurrentUser())
                .flatMap(orgId -> fiscalDocumentRepository.findBySaleIdAndOrganizationId(saleId, orgId))
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Fiscal document not found for sale"));
    }

    public FiscalConfigResponse getFiscalConfig(Long organizationId) {
        validateOrganizationAccess(organizationId);
        entitlementService.assertModule(organizationId, ModuleCode.FISCAL);

        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));

        return mapToFiscalConfigResponse(organization);
    }

    @Transactional
    public FiscalConfigResponse updateFiscalConfig(Long organizationId, FiscalConfigRequest request) {
        validateOrganizationAccess(organizationId);
        entitlementService.assertModule(organizationId, ModuleCode.FISCAL);

        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));

        if (request.getCuit() != null) {
            if (isSpainJurisdiction(organization)) {
                String normalizedNif = SpainTaxIdValidator.normalize(request.getCuit());
                if (StringUtils.hasText(normalizedNif)) {
                    if (!SpainTaxIdValidator.isValid(normalizedNif)) {
                        throw new IllegalArgumentException("NIF/CIF format is invalid");
                    }
                    organization.setCuit(normalizedNif);
                } else {
                    organization.setCuit(null);
                }
            } else {
                String normalizedCuit = TaxIdNormalizer.normalizeCuit(request.getCuit());
                if (StringUtils.hasText(normalizedCuit)) {
                    validateCuitFormat(normalizedCuit);
                    organization.setCuit(normalizedCuit);
                } else {
                    organization.setCuit(null);
                }
            }
        }

        if (StringUtils.hasText(request.getRazonSocial())) {
            organization.setRazonSocial(request.getRazonSocial().trim());
        }

        if (request.getCondicionIva() != null) {
            organization.setCondicionIva(request.getCondicionIva());
        }

        if (request.getFiscalPuntoVenta() != null) {
            organization.setFiscalPuntoVenta(request.getFiscalPuntoVenta());
        }

        if (request.getFiscalProvider() != null) {
            if (isSpainJurisdiction(organization) && request.getFiscalProvider() == FiscalProviderType.TUSFACTURAS) {
                throw new IllegalArgumentException("TusFacturas is not available for Spanish organizations");
            }
            organization.setFiscalProvider(request.getFiscalProvider());
        }

        if (StringUtils.hasText(request.getFiscalApiKey())) {
            organization.setFiscalApiKey(request.getFiscalApiKey().trim());
        }

        Organization saved = organizationRepository.save(organization);
        return mapToFiscalConfigResponse(saved);
    }

    private void validateFiscalConfig(Organization organization) {
        if (isSpainJurisdiction(organization)) {
            validateSpainFiscalConfig(organization);
            return;
        }

        if (!StringUtils.hasText(TaxIdNormalizer.normalizeCuit(organization.getCuit()))) {
            throw new IllegalStateException("Organization CUIT is required for invoicing");
        }

        if (resolvePuntoVenta(organization) == null) {
            throw new IllegalStateException("Punto de venta is required for invoicing");
        }
    }

    private void validateSpainFiscalConfig(Organization organization) {
        String nif = SpainTaxIdValidator.normalize(organization.getCuit());
        if (!SpainTaxIdValidator.isValid(nif)) {
            throw new IllegalStateException("Organization NIF/CIF is required for invoicing");
        }

        if (!StringUtils.hasText(organization.getRazonSocial())) {
            throw new IllegalStateException("Business name is required for invoicing");
        }
    }

    private boolean isSpainJurisdiction(Organization organization) {
        return organization.getFiscalJurisdiction() == FiscalJurisdiction.ES_VERIFACTU;
    }

    private Integer resolveSpainSeries(Organization organization) {
        if (organization.getFiscalPuntoVenta() != null) {
            return organization.getFiscalPuntoVenta();
        }

        return 1;
    }

    private Integer resolvePuntoVenta(Organization organization) {
        Long storeId = TenantContext.getStoreId();
        if (storeId != null) {
            Store store = storeRepository.findById(storeId).orElse(null);
            if (store != null && store.getAfipPuntoVenta() != null) {
                return store.getAfipPuntoVenta();
            }
        }

        if (organization.getFiscalPuntoVenta() != null) {
            return organization.getFiscalPuntoVenta();
        }

        return storeRepository.findAllByOrganization(organization).stream()
                .filter(store -> store.getAfipPuntoVenta() != null)
                .map(Store::getAfipPuntoVenta)
                .findFirst()
                .orElse(null);
    }

    private Store resolveStore(Organization organization) {
        Long storeId = TenantContext.getStoreId();
        if (storeId == null) {
            return null;
        }

        return storeRepository.findById(storeId)
                .filter(store -> store.getOrganization().getId().equals(organization.getId()))
                .orElse(null);
    }

    private Customer resolveCustomer(Long customerId) {
        if (customerId == null) {
            return null;
        }

        return tenantQueryService.findCustomerById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
    }

    private InvoiceTotals calculateSpainTotals(Sale sale) {
        SpainFiscalInvoiceCalculator.InvoiceTotals totals =
                SpainFiscalInvoiceCalculator.calculateFromTotal(sale.getTotalAmount());

        return new InvoiceTotals(totals.neto(), totals.iva(), totals.total());
    }

    private InvoiceTotals calculateTotals(Sale sale, boolean includesIvaBreakdown) {
        FiscalInvoiceCalculator.InvoiceAmounts totals =
                new FiscalInvoiceCalculator.InvoiceAmounts(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);

        for (SaleItem item : sale.getItems()) {
            ProductIvaRate ivaRate = item.getProduct() != null
                    ? item.getProduct().getIvaRate()
                    : ProductIvaRate.GENERAL_21;

            FiscalInvoiceCalculator.InvoiceAmounts lineAmounts = FiscalInvoiceCalculator.calculateFromGrossLine(
                    item.getSubtotal(),
                    ivaRate,
                    includesIvaBreakdown
            );

            totals = totals.add(lineAmounts);
        }

        return new InvoiceTotals(totals.neto(), totals.iva(), totals.total());
    }

    private EmitInvoiceCommand buildCommand(
            Organization organization,
            Customer customer,
            TipoComprobante tipoComprobante,
            Integer puntoVenta,
            Long numeroComprobante,
            InvoiceTotals totals,
            Sale sale,
            boolean includesIvaBreakdown,
            boolean spain
    ) {
        String customerDocumentType;
        String customerDocumentNumber;
        String customerRazonSocial;
        CondicionIva customerCondicionIva;
        String customerEmail;
        String customerAddress;

        if (customer != null) {
            customerDocumentType = customer.getDocumentType().name();
            customerDocumentNumber = spain
                    ? SpainTaxIdValidator.normalize(customer.getDocumentNumber())
                    : TaxIdNormalizer.normalizeDocumentNumber(
                            customer.getDocumentType(),
                            customer.getDocumentNumber()
                    );
            customerRazonSocial = customer.getRazonSocial();
            customerCondicionIva = customer.getCondicionIva();
            customerEmail = customer.getEmail();
            customerAddress = customer.getAddress();
        } else {
            customerDocumentType = spain ? "NIF" : "OTRO";
            customerDocumentNumber = CF_DOCUMENT_NUMBER;
            customerRazonSocial = spain ? ES_CF_RAZON_SOCIAL : CF_RAZON_SOCIAL;
            customerCondicionIva = CondicionIva.CONSUMIDOR_FINAL;
            customerEmail = null;
            customerAddress = null;
        }

        List<EmitInvoiceCommand.InvoiceLineCommand> lines = new ArrayList<>();

        for (SaleItem item : sale.getItems()) {
            ProductIvaRate ivaRate = item.getProduct() != null
                    ? item.getProduct().getIvaRate()
                    : ProductIvaRate.GENERAL_21;

            FiscalInvoiceCalculator.InvoiceAmounts lineAmounts = FiscalInvoiceCalculator.calculateFromGrossLine(
                    item.getSubtotal(),
                    ivaRate,
                    includesIvaBreakdown
            );

            String description = item.getProduct() != null
                    ? item.getProduct().getName()
                    : item.getCustomName();

            lines.add(EmitInvoiceCommand.InvoiceLineCommand.builder()
                    .description(description)
                    .quantity(item.getQuantity())
                    .unitPrice(item.getUnitPrice())
                    .subtotal(item.getSubtotal())
                    .ivaRate(FiscalInvoiceCalculator.ivaRateFactor(ivaRate))
                    .netAmount(lineAmounts.neto())
                    .ivaAmount(lineAmounts.iva())
                    .build());
        }

        return EmitInvoiceCommand.builder()
                .emitterCuit(spain
                        ? SpainTaxIdValidator.normalize(organization.getCuit())
                        : TaxIdNormalizer.normalizeCuit(organization.getCuit()))
                .emitterRazonSocial(organization.getRazonSocial())
                .puntoVenta(puntoVenta)
                .numeroComprobante(numeroComprobante)
                .tipoComprobante(tipoComprobante)
                .customerDocumentType(customerDocumentType)
                .customerDocumentNumber(customerDocumentNumber)
                .customerRazonSocial(customerRazonSocial)
                .customerCondicionIva(customerCondicionIva)
                .customerEmail(customerEmail)
                .customerAddress(customerAddress)
                .importeNeto(totals.neto())
                .importeIva(totals.iva())
                .importeTotal(totals.total())
                .lines(lines)
                .build();
    }

    private Organization resolveOrganizationForInvoicing(Sale sale, User user) {
        Long contextOrgId = tenantScopeService.resolveOrganizationId(user)
                .orElseThrow(() -> new IllegalStateException("Organization context is required for invoicing"));

        if (sale.getOrganization() != null && !sale.getOrganization().getId().equals(contextOrgId)) {
            throw new ResourceNotFoundException("Sale not found");
        }

        return organizationRepository.findById(contextOrgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));
    }

    private void validateCuitFormat(String normalizedCuit) {
        if (normalizedCuit.length() != 11 || !normalizedCuit.matches("\\d{11}")) {
            throw new IllegalArgumentException("CUIT must contain 11 digits");
        }
    }

    private void validateOrganizationAccess(Long organizationId) {
        Long currentOrgId = tenantScopeService.resolveOrganizationId(tenantScopeService.getCurrentUser())
                .orElse(null);

        if (currentOrgId == null || !currentOrgId.equals(organizationId)) {
            throw new ResourceNotFoundException("Organization not found");
        }
    }

    private FiscalConfigResponse mapToFiscalConfigResponse(Organization organization) {
        return FiscalConfigResponse.builder()
                .cuit(isSpainJurisdiction(organization)
                        ? SpainTaxIdValidator.normalize(organization.getCuit())
                        : TaxIdNormalizer.normalizeCuit(organization.getCuit()))
                .razonSocial(organization.getRazonSocial())
                .condicionIva(organization.getCondicionIva())
                .fiscalPuntoVenta(organization.getFiscalPuntoVenta())
                .fiscalProvider(organization.getFiscalProvider())
                .hasFiscalApiKey(StringUtils.hasText(organization.getFiscalApiKey()))
                .countryCode(organization.getCountryCode())
                .fiscalJurisdiction(organization.getFiscalJurisdiction())
                .build();
    }

    private FiscalDocumentResponse mapToResponse(FiscalDocument document) {
        return FiscalDocumentResponse.builder()
                .id(document.getId())
                .organizationId(document.getOrganization().getId())
                .storeId(document.getStore() != null ? document.getStore().getId() : null)
                .saleId(document.getSale() != null ? document.getSale().getId() : null)
                .customerId(document.getCustomer() != null ? document.getCustomer().getId() : null)
                .customerRazonSocial(
                        document.getCustomer() != null
                                ? document.getCustomer().getRazonSocial()
                                : CF_RAZON_SOCIAL
                )
                .tipoComprobante(document.getTipoComprobante())
                .puntoVenta(document.getPuntoVenta())
                .numeroComprobante(document.getNumeroComprobante())
                .cae(document.getCae())
                .caeVencimiento(document.getCaeVencimiento())
                .importeNeto(document.getImporteNeto())
                .importeIva(document.getImporteIva())
                .importeTotal(document.getImporteTotal())
                .status(document.getStatus())
                .rejectionReason(
                        StringUtils.hasText(document.getRejectionReason())
                                ? document.getRejectionReason()
                                : extractRejectionReason(document.getAfipRawJson())
                )
                .pdfUrl(document.getPdfUrl())
                .createdAt(document.getCreatedAt())
                .build();
    }

    private String resolveRejectionReason(EmitInvoiceResult result) {
        if (result.isAuthorized()) {
            return null;
        }

        if (StringUtils.hasText(result.getRejectionReason())) {
            return result.getRejectionReason().trim();
        }

        return extractRejectionReason(result.getRawJson());
    }

    private String extractRejectionReason(String afipRawJson) {
        if (!StringUtils.hasText(afipRawJson)) {
            return null;
        }

        try {
            JsonNode root = objectMapper.readTree(afipRawJson);

            if ("MOCK".equals(root.path("provider").asText(null))) {
                return null;
            }

            String explicitReason = root.path("rejectionReason").asText(null);
            if (StringUtils.hasText(explicitReason)) {
                return explicitReason.trim();
            }

            if ("S".equalsIgnoreCase(root.path("error").asText(""))) {
                String fromErrors = extractTusFacturasErrors(root);
                if (StringUtils.hasText(fromErrors)) {
                    return fromErrors;
                }
                return "TusFacturas rechazó el comprobante";
            }

            return null;
        } catch (Exception ex) {
            return afipRawJson.length() <= 500 ? afipRawJson.trim() : afipRawJson.substring(0, 500).trim() + "...";
        }
    }

    private String extractTusFacturasErrors(JsonNode root) {
        JsonNode errores = root.path("errores");
        if (errores.isArray() && !errores.isEmpty()) {
            String joined = joinJsonTextArray(errores);
            if (StringUtils.hasText(joined)) {
                return joined;
            }
        }

        JsonNode errorDetails = root.path("error_details");
        if (errorDetails.isArray() && !errorDetails.isEmpty()) {
            StringBuilder builder = new StringBuilder();
            for (JsonNode node : errorDetails) {
                String text = node.path("text").asText("").trim();
                if (StringUtils.hasText(text)) {
                    if (!builder.isEmpty()) {
                        builder.append("; ");
                    }
                    builder.append(text);
                }
            }
            if (!builder.isEmpty()) {
                return builder.toString();
            }
        }

        return null;
    }

    private String joinJsonTextArray(JsonNode array) {
        StringBuilder builder = new StringBuilder();
        for (JsonNode node : array) {
            String text = node.asText("").trim();
            if (StringUtils.hasText(text)) {
                if (!builder.isEmpty()) {
                    builder.append("; ");
                }
                builder.append(text);
            }
        }
        return builder.toString();
    }

    private void assertFiscalModule() {
        tenantScopeService.resolveOrganizationId(tenantScopeService.getCurrentUser())
                .ifPresent(orgId -> entitlementService.assertModule(orgId, ModuleCode.FISCAL));
    }

    private record InvoiceTotals(BigDecimal neto, BigDecimal iva, BigDecimal total) {
    }
}
