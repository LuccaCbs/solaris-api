package com.luccavergara.solaris.service;

import com.luccavergara.solaris.dto.CustomerRequest;
import com.luccavergara.solaris.dto.CustomerResponse;
import com.luccavergara.solaris.entity.AuditAction;
import com.luccavergara.solaris.entity.AuditEntityType;
import com.luccavergara.solaris.entity.Customer;
import com.luccavergara.solaris.entity.DocumentType;
import com.luccavergara.solaris.entity.ModuleCode;
import com.luccavergara.solaris.entity.User;
import com.luccavergara.solaris.exception.ResourceNotFoundException;
import com.luccavergara.solaris.repository.CustomerRepository;
import com.luccavergara.solaris.util.TaxIdNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final AuthenticatedUserService authenticatedUserService;
    private final AuditLogService auditLogService;
    private final TenantQueryService tenantQueryService;
    private final TenantScopeService tenantScopeService;
    private final EntitlementService entitlementService;

    public CustomerResponse createCustomer(CustomerRequest request) {
        assertCustomersModule();
        User currentUser = authenticatedUserService.getCurrentUser();
        NormalizedCustomerRequest normalizedRequest = normalizeRequest(request);

        validateUniqueDocument(
                currentUser,
                normalizedRequest.documentType(),
                normalizedRequest.documentNumber(),
                null
        );

        LocalDateTime now = LocalDateTime.now();

        Customer customer = Customer.builder()
                .documentType(normalizedRequest.documentType())
                .documentNumber(normalizedRequest.documentNumber())
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

        tenantScopeService.getOrganizationReference(currentUser)
                .ifPresent(organization -> {
                    customer.setOrganization(organization);
                    customer.setCreatedBy(currentUser);
                });

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

    public CustomerResponse getCustomerById(Long id) {
        assertCustomersModule();

        Customer customer = tenantQueryService.findCustomerById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        return mapToResponse(customer);
    }

    public CustomerResponse updateCustomer(Long id, CustomerRequest request) {
        assertCustomersModule();
        User currentUser = authenticatedUserService.getCurrentUser();
        NormalizedCustomerRequest normalizedRequest = normalizeRequest(request);

        Customer customer = tenantQueryService.findCustomerById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        validateUniqueDocument(
                currentUser,
                normalizedRequest.documentType(),
                normalizedRequest.documentNumber(),
                id
        );

        customer.setDocumentType(normalizedRequest.documentType());
        customer.setDocumentNumber(normalizedRequest.documentNumber());
        customer.setRazonSocial(normalizedRequest.razonSocial());
        customer.setEmail(normalizedRequest.email());
        customer.setPhone(normalizedRequest.phone());
        customer.setAddress(normalizedRequest.address());
        customer.setCondicionIva(normalizedRequest.condicionIva());
        customer.setUpdatedAt(LocalDateTime.now());

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

    private NormalizedCustomerRequest normalizeRequest(CustomerRequest request) {
        DocumentType documentType = request.getDocumentType();
        TaxIdNormalizer.validateDocumentNumber(documentType, request.getDocumentNumber());

        String documentNumber = TaxIdNormalizer.normalizeDocumentNumber(
                documentType,
                request.getDocumentNumber()
        );
        String phone = TaxIdNormalizer.normalizePhone(request.getPhone());
        TaxIdNormalizer.validatePhone(phone);

        return new NormalizedCustomerRequest(
                documentType,
                documentNumber,
                request.getRazonSocial().trim(),
                TaxIdNormalizer.normalizeEmail(request.getEmail()),
                phone,
                request.getAddress() != null ? request.getAddress().trim() : null,
                request.getCondicionIva()
        );
    }

    private void validateUniqueDocument(
            User currentUser,
            DocumentType documentType,
            String documentNumber,
            Long excludeId
    ) {
        tenantScopeService.resolveOrganizationId(currentUser)
                .ifPresentOrElse(
                        organizationId -> {
                            boolean exists = excludeId == null
                                    ? customerRepository.existsByDocumentTypeAndDocumentNumberAndOrganizationId(
                                    documentType,
                                    documentNumber,
                                    organizationId
                            )
                                    : customerRepository.existsByDocumentTypeAndDocumentNumberAndOrganizationIdAndIdNot(
                                    documentType,
                                    documentNumber,
                                    organizationId,
                                    excludeId
                            );

                            if (exists) {
                                throw new IllegalArgumentException("A customer with this document already exists");
                            }
                        },
                        () -> {
                            boolean exists = excludeId == null
                                    ? customerRepository.existsByDocumentTypeAndDocumentNumberAndUserAndOrganizationIsNull(
                                    documentType,
                                    documentNumber,
                                    currentUser
                            )
                                    : customerRepository.existsByDocumentTypeAndDocumentNumberAndUserAndOrganizationIsNullAndIdNot(
                                    documentType,
                                    documentNumber,
                                    currentUser,
                                    excludeId
                            );

                            if (exists) {
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
        return CustomerResponse.builder()
                .id(customer.getId())
                .documentType(customer.getDocumentType())
                .documentNumber(customer.getDocumentNumber())
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

    private record NormalizedCustomerRequest(
            DocumentType documentType,
            String documentNumber,
            String razonSocial,
            String email,
            String phone,
            String address,
            com.luccavergara.solaris.entity.CondicionIva condicionIva
    ) {
    }
}
