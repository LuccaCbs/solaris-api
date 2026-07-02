package com.luccavergara.solaris.service;

import com.luccavergara.solaris.dto.CustomerRequest;
import com.luccavergara.solaris.dto.CustomerResponse;
import com.luccavergara.solaris.entity.AuditAction;
import com.luccavergara.solaris.entity.AuditEntityType;
import com.luccavergara.solaris.entity.Customer;
import com.luccavergara.solaris.entity.User;
import com.luccavergara.solaris.exception.ResourceNotFoundException;
import com.luccavergara.solaris.repository.CustomerRepository;
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

    public CustomerResponse createCustomer(CustomerRequest request) {
        User currentUser = authenticatedUserService.getCurrentUser();
        LocalDateTime now = LocalDateTime.now();

        Customer customer = Customer.builder()
                .documentType(request.getDocumentType())
                .documentNumber(request.getDocumentNumber())
                .razonSocial(request.getRazonSocial())
                .email(request.getEmail())
                .phone(request.getPhone())
                .address(request.getAddress())
                .condicionIva(request.getCondicionIva())
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

    public List<CustomerResponse> getAllCustomers() {
        return tenantQueryService.findAllCustomers()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public CustomerResponse getCustomerById(Long id) {
        Customer customer = tenantQueryService.findCustomerById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        return mapToResponse(customer);
    }

    public CustomerResponse updateCustomer(Long id, CustomerRequest request) {
        Customer customer = tenantQueryService.findCustomerById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        customer.setDocumentType(request.getDocumentType());
        customer.setDocumentNumber(request.getDocumentNumber());
        customer.setRazonSocial(request.getRazonSocial());
        customer.setEmail(request.getEmail());
        customer.setPhone(request.getPhone());
        customer.setAddress(request.getAddress());
        customer.setCondicionIva(request.getCondicionIva());
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

    public void deleteCustomer(Long id) {
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
                .createdAt(customer.getCreatedAt())
                .updatedAt(customer.getUpdatedAt())
                .build();
    }
}
