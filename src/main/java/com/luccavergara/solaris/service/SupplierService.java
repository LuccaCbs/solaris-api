package com.luccavergara.solaris.service;

import com.luccavergara.solaris.dto.SupplierRequest;
import com.luccavergara.solaris.dto.SupplierResponse;
import com.luccavergara.solaris.entity.Supplier;
import com.luccavergara.solaris.entity.User;
import com.luccavergara.solaris.exception.ResourceNotFoundException;
import com.luccavergara.solaris.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.luccavergara.solaris.entity.AuditAction;
import com.luccavergara.solaris.entity.AuditEntityType;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SupplierService {

    private final SupplierRepository supplierRepository;
    private final AuthenticatedUserService authenticatedUserService;
    private final AuditLogService auditLogService;
    private final TenantQueryService tenantQueryService;
    private final TenantScopeService tenantScopeService;

    public SupplierResponse createSupplier(SupplierRequest request) {
        User currentUser = authenticatedUserService.getCurrentUser();
        LocalDateTime now = LocalDateTime.now();

        Supplier supplier = Supplier.builder()
                .name(request.getName())
                .contactName(request.getContactName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .address(request.getAddress())
                .notes(request.getNotes())
                .active(request.getActive() != null ? request.getActive() : true)
                .createdAt(now)
                .updatedAt(now)
                .user(currentUser)
                .build();

        tenantScopeService.getOrganizationReference(currentUser)
                .ifPresent(organization -> {
                    supplier.setOrganization(organization);
                    supplier.setCreatedBy(currentUser);
                });

        Supplier savedSupplier = supplierRepository.save(supplier);

        auditLogService.log(
                AuditAction.CREATE,
                AuditEntityType.SUPPLIER,
                savedSupplier.getId(),
                savedSupplier.getName(),
                "Supplier created"
        );

        return mapToResponse(savedSupplier);
    }

    public List<SupplierResponse> getAllSuppliers(Boolean active) {
        User currentUser = authenticatedUserService.getCurrentUser();

        List<Supplier> suppliers;

        if (active == null || Boolean.TRUE.equals(active)) {
            suppliers = tenantScopeService.resolveOrganizationId(currentUser)
                    .map(supplierRepository::findAllByOrganizationIdAndActiveTrueOrderByCreatedAtDesc)
                    .orElseGet(() -> supplierRepository.findAllByUserAndActiveTrueOrderByCreatedAtDesc(currentUser));
        } else {
            suppliers = tenantScopeService.resolveOrganizationId(currentUser)
                    .map(supplierRepository::findAllByOrganizationIdOrderByCreatedAtDesc)
                    .orElseGet(() -> supplierRepository.findAllByUserOrderByCreatedAtDesc(currentUser));

            suppliers = suppliers.stream()
                    .filter(supplier -> Boolean.FALSE.equals(supplier.getActive()))
                    .toList();
        }

        return suppliers.stream()
                .map(this::mapToResponse)
                .toList();
    }

    public SupplierResponse getSupplierById(Long id) {
        User currentUser = authenticatedUserService.getCurrentUser();

        Supplier supplier = tenantQueryService.findSupplierById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found"));

        return mapToResponse(supplier);
    }

    public SupplierResponse updateSupplier(Long id, SupplierRequest request) {
        User currentUser = authenticatedUserService.getCurrentUser();

        Supplier supplier = tenantQueryService.findSupplierById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found"));

        supplier.setName(request.getName());
        supplier.setContactName(request.getContactName());
        supplier.setEmail(request.getEmail());
        supplier.setPhone(request.getPhone());
        supplier.setAddress(request.getAddress());
        supplier.setNotes(request.getNotes());

        if (request.getActive() != null) {
            supplier.setActive(request.getActive());
        }

        supplier.setUpdatedAt(LocalDateTime.now());

        Supplier updatedSupplier = supplierRepository.save(supplier);

        auditLogService.log(
                AuditAction.UPDATE,
                AuditEntityType.SUPPLIER,
                updatedSupplier.getId(),
                updatedSupplier.getName(),
                "Supplier updated"
        );

        return mapToResponse(updatedSupplier);
    }

    public SupplierResponse deactivateSupplier(Long id) {
        User currentUser = authenticatedUserService.getCurrentUser();

        Supplier supplier = tenantQueryService.findSupplierById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found"));

        supplier.setActive(false);
        supplier.setUpdatedAt(LocalDateTime.now());

        Supplier updatedSupplier = supplierRepository.save(supplier);

        auditLogService.log(
                AuditAction.UPDATE,
                AuditEntityType.SUPPLIER,
                updatedSupplier.getId(),
                updatedSupplier.getName(),
                "Supplier deactivated"
        );

        return mapToResponse(updatedSupplier);
    }

    public SupplierResponse activateSupplier(Long id) {
        User currentUser = authenticatedUserService.getCurrentUser();

        Supplier supplier = tenantQueryService.findSupplierById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found"));

        supplier.setActive(true);
        supplier.setUpdatedAt(LocalDateTime.now());

        Supplier updatedSupplier = supplierRepository.save(supplier);

        auditLogService.log(
                AuditAction.UPDATE,
                AuditEntityType.SUPPLIER,
                updatedSupplier.getId(),
                updatedSupplier.getName(),
                "Supplier activated"
        );

        return mapToResponse(updatedSupplier);
    }

    public void deleteSupplier(Long id) {
        User currentUser = authenticatedUserService.getCurrentUser();

        Supplier supplier = tenantQueryService.findSupplierById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found"));

        auditLogService.log(
                AuditAction.DELETE,
                AuditEntityType.SUPPLIER,
                supplier.getId(),
                supplier.getName(),
                "Supplier deleted"
        );

        supplierRepository.delete(supplier);
    }

    private SupplierResponse mapToResponse(Supplier supplier) {
        return SupplierResponse.builder()
                .id(supplier.getId())
                .name(supplier.getName())
                .contactName(supplier.getContactName())
                .email(supplier.getEmail())
                .phone(supplier.getPhone())
                .address(supplier.getAddress())
                .notes(supplier.getNotes())
                .active(supplier.getActive())
                .createdAt(supplier.getCreatedAt())
                .updatedAt(supplier.getUpdatedAt())
                .build();
    }
}