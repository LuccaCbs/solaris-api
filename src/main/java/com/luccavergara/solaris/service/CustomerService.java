package com.luccavergara.solaris.service;

import com.luccavergara.solaris.dto.CustomerDocumentRequest;
import com.luccavergara.solaris.dto.CustomerDocumentResponse;
import com.luccavergara.solaris.dto.CustomerPreviewResponse;
import com.luccavergara.solaris.dto.CustomerRequest;
import com.luccavergara.solaris.dto.CustomerResponse;
import com.luccavergara.solaris.dto.FiscalDocumentResponse;
import com.luccavergara.solaris.entity.AuditAction;
import com.luccavergara.solaris.entity.AuditEntityType;
import com.luccavergara.solaris.entity.Customer;
import com.luccavergara.solaris.entity.CustomerDocument;
import com.luccavergara.solaris.entity.DocumentType;
import com.luccavergara.solaris.entity.ModuleCode;
import com.luccavergara.solaris.entity.Organization;
import com.luccavergara.solaris.entity.User;
import com.luccavergara.solaris.exception.ResourceNotFoundException;
import com.luccavergara.solaris.repository.CustomerDocumentRepository;
import com.luccavergara.solaris.repository.CustomerRepository;
import com.luccavergara.solaris.util.TaxIdNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerDocumentRepository customerDocumentRepository;
    private final AuthenticatedUserService authenticatedUserService;
    private final AuditLogService auditLogService;
    private final TenantQueryService tenantQueryService;
    private final TenantScopeService tenantScopeService;
    private final EntitlementService entitlementService;
    private final FiscalDocumentService fiscalDocumentService;

    public CustomerResponse createCustomer(CustomerRequest request) {
        assertCustomersModule();
        User currentUser = authenticatedUserService.getCurrentUser();
        NormalizedCustomerRequest normalizedRequest = normalizeRequest(request, currentUser, null);

        LocalDateTime now = LocalDateTime.now();
        Organization organization = tenantScopeService.getOrganizationReference(currentUser).orElse(null);

        Customer customer = Customer.builder()
                .documentType(normalizedRequest.primaryDocument().documentType())
                .documentNumber(normalizedRequest.primaryDocument().documentNumber())
                .razonSocial(normalizedRequest.razonSocial())
                .email(normalizedRequest.email())
                .phone(normalizedRequest.phone())
                .address(normalizedRequest.address())
                .condicionIva(normalizedRequest.condicionIva())
                .active(true)
                .createdAt(now)
                .updatedAt(now)
                .user(currentUser)
                .build();

        if (organization != null) {
            customer.setOrganization(organization);
            customer.setCreatedBy(currentUser);
        }

        applyDocuments(customer, normalizedRequest.documents(), currentUser, organization, now);

        Customer savedCustomer = customerRepository.save(customer);

        auditLogService.log(
                AuditAction.CREATE,
                AuditEntityType.CUSTOMER,
                savedCustomer.getId(),
                savedCustomer.getRazonSocial(),
                "Customer created"
        );

        return mapToResponse(savedCustomer);
    }

    @Transactional(readOnly = true)
    public List<CustomerResponse> getAllCustomers(Boolean active) {
        assertCustomersModule();
        User currentUser = authenticatedUserService.getCurrentUser();

        List<Customer> customers;

        if (active == null || Boolean.TRUE.equals(active)) {
            customers = tenantScopeService.resolveOrganizationId(currentUser)
                    .map(customerRepository::findAllByOrganizationIdAndActiveTrueOrderByCreatedAtDesc)
                    .orElseGet(() -> customerRepository.findAllByUserAndActiveTrueOrderByCreatedAtDesc(currentUser));
        } else {
            customers = tenantScopeService.resolveOrganizationId(currentUser)
                    .map(customerRepository::findAllByOrganizationIdOrderByCreatedAtDesc)
                    .orElseGet(() -> customerRepository.findAllByUserOrderByCreatedAtDesc(currentUser));

            customers = customers.stream()
                    .filter(customer -> Boolean.FALSE.equals(customer.getActive()))
                    .toList();
        }

        return customers.stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CustomerResponse> searchCustomers(String query) {
        assertCustomersModule();
        User currentUser = authenticatedUserService.getCurrentUser();

        if (query == null || query.isBlank()) {
            return getAllCustomers(null);
        }

        String normalizedQuery = normalizeSearchQuery(query);

        List<Customer> customers = tenantScopeService.resolveOrganizationId(currentUser)
                .map(orgId -> customerRepository.searchActiveByOrganization(orgId, normalizedQuery))
                .orElseGet(() -> customerRepository.searchActiveByUser(currentUser.getId(), normalizedQuery));

        return customers.stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CustomerResponse getCustomerById(Long id) {
        assertCustomersModule();

        Customer customer = tenantQueryService.findCustomerById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        return mapToResponse(customer);
    }

    @Transactional(readOnly = true)
    public CustomerPreviewResponse getCustomerPreview(Long id) {
        assertCustomersModule();

        Customer customer = tenantQueryService.findCustomerById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        List<FiscalDocumentResponse> invoicedDocuments =
                fiscalDocumentService.getFiscalDocumentsByCustomerId(id);

        return CustomerPreviewResponse.builder()
                .customer(mapToResponse(customer))
                .totalInvoicedDocuments((long) invoicedDocuments.size())
                .invoicedDocuments(invoicedDocuments)
                .build();
    }

    public CustomerResponse updateCustomer(Long id, CustomerRequest request) {
        assertCustomersModule();
        User currentUser = authenticatedUserService.getCurrentUser();
        NormalizedCustomerRequest normalizedRequest = normalizeRequest(request, currentUser, id);

        Customer customer = tenantQueryService.findCustomerById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        Organization organization = customer.getOrganization();
        LocalDateTime now = LocalDateTime.now();

        customer.setDocumentType(normalizedRequest.primaryDocument().documentType());
        customer.setDocumentNumber(normalizedRequest.primaryDocument().documentNumber());
        customer.setRazonSocial(normalizedRequest.razonSocial());
        customer.setEmail(normalizedRequest.email());
        customer.setPhone(normalizedRequest.phone());
        customer.setAddress(normalizedRequest.address());
        customer.setCondicionIva(normalizedRequest.condicionIva());
        customer.setUpdatedAt(now);

        customer.getDocuments().clear();
        applyDocuments(customer, normalizedRequest.documents(), currentUser, organization, now);

        Customer updatedCustomer = customerRepository.save(customer);

        auditLogService.log(
                AuditAction.UPDATE,
                AuditEntityType.CUSTOMER,
                updatedCustomer.getId(),
                updatedCustomer.getRazonSocial(),
                "Customer updated"
        );

        return mapToResponse(updatedCustomer);
    }

    public CustomerResponse deactivateCustomer(Long id) {
        assertCustomersModule();

        Customer customer = tenantQueryService.findCustomerById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        customer.setActive(false);
        customer.setUpdatedAt(LocalDateTime.now());

        Customer updatedCustomer = customerRepository.save(customer);

        auditLogService.log(
                AuditAction.UPDATE,
                AuditEntityType.CUSTOMER,
                updatedCustomer.getId(),
                updatedCustomer.getRazonSocial(),
                "Customer deactivated"
        );

        return mapToResponse(updatedCustomer);
    }

    public CustomerResponse activateCustomer(Long id) {
        assertCustomersModule();

        Customer customer = tenantQueryService.findCustomerById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        customer.setActive(true);
        customer.setUpdatedAt(LocalDateTime.now());

        Customer updatedCustomer = customerRepository.save(customer);

        auditLogService.log(
                AuditAction.UPDATE,
                AuditEntityType.CUSTOMER,
                updatedCustomer.getId(),
                updatedCustomer.getRazonSocial(),
                "Customer activated"
        );

        return mapToResponse(updatedCustomer);
    }

    public void deleteCustomer(Long id) {
        assertCustomersModule();

        Customer customer = tenantQueryService.findCustomerById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        auditLogService.log(
                AuditAction.DELETE,
                AuditEntityType.CUSTOMER,
                customer.getId(),
                customer.getRazonSocial(),
                "Customer deleted"
        );

        customerRepository.delete(customer);
    }

    private String normalizeSearchQuery(String query) {
        String trimmed = query.trim();
        String digitsOnly = trimmed.replaceAll("\\D", "");

        if (!digitsOnly.isEmpty() && digitsOnly.matches("\\d{6,}")) {
            return digitsOnly;
        }

        return trimmed;
    }

    private List<CustomerDocumentRequest> resolveDocumentRequests(CustomerRequest request) {
        if (request.getDocuments() != null && !request.getDocuments().isEmpty()) {
            return request.getDocuments();
        }

        if (request.getDocumentType() != null && request.getDocumentNumber() != null) {
            return List.of(
                    CustomerDocumentRequest.builder()
                            .documentType(request.getDocumentType())
                            .documentNumber(request.getDocumentNumber())
                            .primary(true)
                            .build()
            );
        }

        throw new IllegalArgumentException("At least one document is required");
    }

    private NormalizedCustomerRequest normalizeRequest(
            CustomerRequest request,
            User currentUser,
            Long excludeCustomerId
    ) {
        List<CustomerDocumentRequest> documentRequests = resolveDocumentRequests(request);
        List<NormalizedDocument> normalizedDocuments = new ArrayList<>();
        Set<String> seenKeys = new HashSet<>();
        boolean hasPrimary = documentRequests.stream()
                .anyMatch(document -> Boolean.TRUE.equals(document.getPrimary()));

        for (int index = 0; index < documentRequests.size(); index++) {
            CustomerDocumentRequest documentRequest = documentRequests.get(index);
            DocumentType documentType = documentRequest.getDocumentType();
            TaxIdNormalizer.validateDocumentNumber(documentType, documentRequest.getDocumentNumber());

            String documentNumber = TaxIdNormalizer.normalizeDocumentNumber(
                    documentType,
                    documentRequest.getDocumentNumber()
            );
            String key = documentType.name() + ":" + documentNumber;

            if (!seenKeys.add(key)) {
                throw new IllegalArgumentException("Duplicate documents are not allowed");
            }

            validateUniqueDocument(currentUser, documentType, documentNumber, excludeCustomerId);

            boolean primary = Boolean.TRUE.equals(documentRequest.getPrimary())
                    || (!hasPrimary && index == 0);

            normalizedDocuments.add(new NormalizedDocument(documentType, documentNumber, primary));
        }

        ensureSinglePrimary(normalizedDocuments);

        NormalizedDocument primaryDocument = normalizedDocuments.stream()
                .filter(NormalizedDocument::primary)
                .findFirst()
                .orElse(normalizedDocuments.get(0));

        String phone = TaxIdNormalizer.normalizePhone(request.getPhone());
        TaxIdNormalizer.validatePhone(phone);

        return new NormalizedCustomerRequest(
                normalizedDocuments,
                primaryDocument,
                request.getRazonSocial().trim(),
                TaxIdNormalizer.normalizeEmail(request.getEmail()),
                phone,
                request.getAddress() != null ? request.getAddress().trim() : null,
                request.getCondicionIva()
        );
    }

    private void ensureSinglePrimary(List<NormalizedDocument> documents) {
        long primaryCount = documents.stream().filter(NormalizedDocument::primary).count();

        if (primaryCount == 0) {
            NormalizedDocument first = documents.get(0);
            documents.set(0, new NormalizedDocument(
                    first.documentType(),
                    first.documentNumber(),
                    true
            ));
            return;
        }

        if (primaryCount > 1) {
            throw new IllegalArgumentException("Only one document can be marked as primary");
        }
    }

    private void applyDocuments(
            Customer customer,
            List<NormalizedDocument> documents,
            User currentUser,
            Organization organization,
            LocalDateTime createdAt
    ) {
        for (NormalizedDocument document : documents) {
            CustomerDocument customerDocument = CustomerDocument.builder()
                    .customer(customer)
                    .documentType(document.documentType())
                    .documentNumber(document.documentNumber())
                    .primary(document.primary())
                    .user(currentUser)
                    .organization(organization)
                    .createdAt(createdAt)
                    .build();

            customer.getDocuments().add(customerDocument);
        }
    }

    private void validateUniqueDocument(
            User currentUser,
            DocumentType documentType,
            String documentNumber,
            Long excludeCustomerId
    ) {
        tenantScopeService.resolveOrganizationId(currentUser)
                .ifPresentOrElse(
                        organizationId -> {
                            if (customerDocumentRepository.existsByDocumentInOrganization(
                                    organizationId,
                                    documentType,
                                    documentNumber,
                                    excludeCustomerId
                            )) {
                                throw new IllegalArgumentException("A customer with this document already exists");
                            }
                        },
                        () -> {
                            if (customerDocumentRepository.existsByDocumentForPersonalUser(
                                    currentUser,
                                    documentType,
                                    documentNumber,
                                    excludeCustomerId
                            )) {
                                throw new IllegalArgumentException("A customer with this document already exists");
                            }
                        }
                );
    }

    private void assertCustomersModule() {
        User currentUser = authenticatedUserService.getCurrentUser();
        tenantScopeService.resolveOrganizationId(currentUser)
                .ifPresent(orgId -> entitlementService.assertModule(orgId, ModuleCode.CUSTOMERS));
    }

    private CustomerResponse mapToResponse(Customer customer) {
        List<CustomerDocumentResponse> documents = customer.getDocuments().stream()
                .map(document -> CustomerDocumentResponse.builder()
                        .id(document.getId())
                        .documentType(document.getDocumentType())
                        .documentNumber(document.getDocumentNumber())
                        .primary(document.getPrimary())
                        .build())
                .toList();

        CustomerDocumentResponse primaryDocument = documents.stream()
                .filter(document -> Boolean.TRUE.equals(document.getPrimary()))
                .findFirst()
                .orElse(documents.isEmpty() ? null : documents.get(0));

        DocumentType documentType = primaryDocument != null
                ? primaryDocument.getDocumentType()
                : customer.getDocumentType();
        String documentNumber = primaryDocument != null
                ? primaryDocument.getDocumentNumber()
                : customer.getDocumentNumber();

        return CustomerResponse.builder()
                .id(customer.getId())
                .documentType(documentType)
                .documentNumber(documentNumber)
                .documents(documents)
                .razonSocial(customer.getRazonSocial())
                .email(customer.getEmail())
                .phone(customer.getPhone())
                .address(customer.getAddress())
                .condicionIva(customer.getCondicionIva())
                .active(customer.getActive())
                .createdAt(customer.getCreatedAt())
                .updatedAt(customer.getUpdatedAt())
                .build();
    }

    private record NormalizedDocument(
            DocumentType documentType,
            String documentNumber,
            boolean primary
    ) {
    }

    private record NormalizedCustomerRequest(
            List<NormalizedDocument> documents,
            NormalizedDocument primaryDocument,
            String razonSocial,
            String email,
            String phone,
            String address,
            com.luccavergara.solaris.entity.CondicionIva condicionIva
    ) {
    }
}
