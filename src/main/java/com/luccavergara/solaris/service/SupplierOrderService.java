package com.luccavergara.solaris.service;

import com.luccavergara.solaris.dto.StockMovementRequest;
import com.luccavergara.solaris.dto.SupplierOrderItemResponse;
import com.luccavergara.solaris.dto.SupplierOrderRequest;
import com.luccavergara.solaris.dto.SupplierOrderResponse;
import com.luccavergara.solaris.entity.Product;
import com.luccavergara.solaris.entity.Supplier;
import com.luccavergara.solaris.entity.SupplierOrder;
import com.luccavergara.solaris.entity.SupplierOrderItem;
import com.luccavergara.solaris.entity.StockMovementType;
import com.luccavergara.solaris.entity.SupplierOrderStatus;
import com.luccavergara.solaris.entity.User;
import com.luccavergara.solaris.exception.ResourceNotFoundException;
import com.luccavergara.solaris.repository.ProductRepository;
import com.luccavergara.solaris.repository.SupplierOrderRepository;
import com.luccavergara.solaris.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.luccavergara.solaris.entity.AuditAction;
import com.luccavergara.solaris.entity.AuditEntityType;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SupplierOrderService {

    private final SupplierOrderRepository supplierOrderRepository;
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;
    private final AuthenticatedUserService authenticatedUserService;
    private final AuditLogService auditLogService;
    private final TenantQueryService tenantQueryService;
    private final TenantScopeService tenantScopeService;
    private final StockMovementService stockMovementService;

    @Transactional
    public SupplierOrderResponse createSupplierOrder(SupplierOrderRequest request) {
        User currentUser = authenticatedUserService.getCurrentUser();

        Supplier supplier = tenantQueryService.findSupplierById(request.getSupplierId())
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found"));

        LocalDateTime now = LocalDateTime.now();

        SupplierOrder supplierOrder = SupplierOrder.builder()
                .supplier(supplier)
                .user(currentUser)
                .status(SupplierOrderStatus.DRAFT)
                .createdAt(now)
                .updatedAt(now)
                .build();

        List<SupplierOrderItem> items = request.getItems()
                .stream()
                .map(itemRequest -> {
                    Product product = tenantQueryService.findProductById(itemRequest.getProductId())
                            .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

                    return SupplierOrderItem.builder()
                            .supplierOrder(supplierOrder)
                            .product(product)
                            .quantity(itemRequest.getQuantity())
                            .build();
                })
                .toList();

        supplierOrder.setItems(items);
        supplierOrder.setMessagePreview(buildMessagePreview(supplier, items));

        tenantScopeService.getOrganizationReference(currentUser)
                .ifPresent(organization -> {
                    supplierOrder.setOrganization(organization);
                    supplierOrder.setCreatedBy(currentUser);
                });

        SupplierOrder savedOrder = supplierOrderRepository.save(supplierOrder);

        auditLogService.log(
                AuditAction.CREATE_SUPPLIER_ORDER,
                AuditEntityType.SUPPLIER_ORDER,
                savedOrder.getId(),
                "Supplier Order #" + savedOrder.getId(),
                "Supplier order created for supplier: " + savedOrder.getSupplier().getName()
        );

        return mapToResponse(savedOrder);
    }

    public List<SupplierOrderResponse> getAllSupplierOrders() {
        User currentUser = authenticatedUserService.getCurrentUser();

        return tenantQueryService.findAllSupplierOrders()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public SupplierOrderResponse updateSupplierOrder(Long id, SupplierOrderRequest request) {
        User currentUser = authenticatedUserService.getCurrentUser();

        SupplierOrder supplierOrder = tenantQueryService.findSupplierOrderById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier order not found"));

        if (supplierOrder.getStatus() != SupplierOrderStatus.DRAFT) {
            throw new IllegalStateException("Only draft supplier orders can be edited");
        }

        Supplier supplier = tenantQueryService.findSupplierById(request.getSupplierId())
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found"));

        List<SupplierOrderItem> items = request.getItems()
                .stream()
                .map(itemRequest -> {
                    Product product = tenantQueryService.findProductById(itemRequest.getProductId())
                            .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

                    return SupplierOrderItem.builder()
                            .supplierOrder(supplierOrder)
                            .product(product)
                            .quantity(itemRequest.getQuantity())
                            .build();
                })
                .toList();

        supplierOrder.getItems().clear();
        supplierOrder.getItems().addAll(items);
        supplierOrder.setSupplier(supplier);
        supplierOrder.setMessagePreview(buildMessagePreview(supplier, items));
        supplierOrder.setUpdatedAt(LocalDateTime.now());

        SupplierOrder savedOrder = supplierOrderRepository.save(supplierOrder);

        auditLogService.log(
                AuditAction.UPDATE,
                AuditEntityType.SUPPLIER_ORDER,
                savedOrder.getId(),
                "Supplier Order #" + savedOrder.getId(),
                "Supplier order updated"
        );

        return mapToResponse(savedOrder);
    }

    public SupplierOrderResponse getSupplierOrderById(Long id) {
        User currentUser = authenticatedUserService.getCurrentUser();

        SupplierOrder supplierOrder = tenantQueryService.findSupplierOrderById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier order not found"));

        return mapToResponse(supplierOrder);
    }

    public List<SupplierOrderResponse> getRecentOrdersBySupplierId(Long supplierId, int limit) {
        tenantQueryService.findSupplierById(supplierId)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found"));

        return supplierOrderRepository.findAllBySupplierIdOrderByCreatedAtDesc(supplierId)
                .stream()
                .limit(limit)
                .map(this::mapToResponse)
                .toList();
    }

    public long countOrdersBySupplierId(Long supplierId) {
        tenantQueryService.findSupplierById(supplierId)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found"));

        return supplierOrderRepository.countBySupplierId(supplierId);
    }

    @Transactional
    public SupplierOrderResponse markAsSent(Long id) {
        User currentUser = authenticatedUserService.getCurrentUser();

        SupplierOrder supplierOrder = tenantQueryService.findSupplierOrderById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier order not found"));

        if (supplierOrder.getStatus() != SupplierOrderStatus.DRAFT) {
            throw new IllegalStateException("Only draft supplier orders can be marked as sent");
        }

        supplierOrder.setStatus(SupplierOrderStatus.SENT);
        supplierOrder.setUpdatedAt(LocalDateTime.now());

        SupplierOrder savedOrder = supplierOrderRepository.save(supplierOrder);

        auditLogService.log(
                AuditAction.UPDATE,
                AuditEntityType.SUPPLIER_ORDER,
                savedOrder.getId(),
                "Supplier Order #" + savedOrder.getId(),
                "Supplier order marked as sent"
        );

        return mapToResponse(savedOrder);
    }

    @Transactional
    public SupplierOrderResponse markAsCompleted(Long id) {
        User currentUser = authenticatedUserService.getCurrentUser();

        SupplierOrder supplierOrder = tenantQueryService.findSupplierOrderById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier order not found"));

        if (supplierOrder.getStatus() == SupplierOrderStatus.COMPLETED) {
            return mapToResponse(supplierOrder);
        }

        if (supplierOrder.getStatus() != SupplierOrderStatus.SENT) {
            throw new IllegalStateException("Only sent supplier orders can be completed");
        }

        String stockReason = "Supplier order #" + supplierOrder.getId() + " completed";

        for (SupplierOrderItem item : supplierOrder.getItems()) {
            stockMovementService.createMovement(
                    StockMovementRequest.builder()
                            .productId(item.getProduct().getId())
                            .type(StockMovementType.IN)
                            .quantity(item.getQuantity())
                            .reason(stockReason)
                            .build()
            );
        }

        supplierOrder.setStatus(SupplierOrderStatus.COMPLETED);
        supplierOrder.setUpdatedAt(LocalDateTime.now());

        SupplierOrder savedOrder = supplierOrderRepository.save(supplierOrder);

        auditLogService.log(
                AuditAction.COMPLETE_SUPPLIER_ORDER,
                AuditEntityType.SUPPLIER_ORDER,
                savedOrder.getId(),
                "Supplier Order #" + savedOrder.getId(),
                "Supplier order completed and stock updated"
        );

        return mapToResponse(savedOrder);
    }

    @Transactional
    public SupplierOrderResponse cancelSupplierOrder(Long id) {
        User currentUser = authenticatedUserService.getCurrentUser();

        SupplierOrder supplierOrder = tenantQueryService.findSupplierOrderById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier order not found"));

        if (supplierOrder.getStatus() == SupplierOrderStatus.COMPLETED) {
            throw new IllegalStateException("Completed supplier orders cannot be cancelled");
        }

        supplierOrder.setStatus(SupplierOrderStatus.CANCELLED);
        supplierOrder.setUpdatedAt(LocalDateTime.now());

        SupplierOrder savedOrder = supplierOrderRepository.save(supplierOrder);

        auditLogService.log(
                AuditAction.CANCEL_SUPPLIER_ORDER,
                AuditEntityType.SUPPLIER_ORDER,
                savedOrder.getId(),
                "Supplier Order #" + savedOrder.getId(),
                "Supplier order cancelled"
        );

        return mapToResponse(savedOrder);
    }

    public void deleteSupplierOrder(Long id) {
        User currentUser = authenticatedUserService.getCurrentUser();

        SupplierOrder supplierOrder = tenantQueryService.findSupplierOrderById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier order not found"));

        if (supplierOrder.getStatus() == SupplierOrderStatus.COMPLETED) {
            throw new IllegalStateException("Completed supplier orders can't be deleted");
        }

        auditLogService.log(
                AuditAction.DELETE,
                AuditEntityType.SUPPLIER_ORDER,
                supplierOrder.getId(),
                "Supplier Order #" + supplierOrder.getId(),
                "Supplier order deleted"
        );

        supplierOrderRepository.delete(supplierOrder);
    }

    private String buildMessagePreview(
            Supplier supplier,
            List<SupplierOrderItem> items
    ) {
        String supplierName = supplier.getContactName() != null && !supplier.getContactName().isBlank()
                ? supplier.getContactName()
                : supplier.getName();

        StringBuilder message = new StringBuilder();

        message.append("Hola ")
                .append(supplierName)
                .append(", te encargo:\n");

        for (SupplierOrderItem item : items) {
            message.append("- ")
                    .append(item.getProduct().getName())
                    .append(" x ")
                    .append(item.getQuantity())
                    .append("\n");
        }

        return message.toString().trim();
    }

    private SupplierOrderResponse mapToResponse(SupplierOrder supplierOrder) {
        return SupplierOrderResponse.builder()
                .id(supplierOrder.getId())
                .supplierId(supplierOrder.getSupplier().getId())
                .supplierName(supplierOrder.getSupplier().getName())
                .supplierPhone(supplierOrder.getSupplier().getPhone())
                .status(supplierOrder.getStatus())
                .messagePreview(supplierOrder.getMessagePreview())
                .items(
                        supplierOrder.getItems()
                                .stream()
                                .map(this::mapItemToResponse)
                                .toList()
                )
                .createdAt(supplierOrder.getCreatedAt())
                .updatedAt(supplierOrder.getUpdatedAt())
                .build();
    }

    private SupplierOrderItemResponse mapItemToResponse(SupplierOrderItem item) {
        return SupplierOrderItemResponse.builder()
                .id(item.getId())
                .productId(item.getProduct().getId())
                .productName(item.getProduct().getName())
                .productBarcode(item.getProduct().getBarcode())
                .quantity(item.getQuantity())
                .build();
    }
}