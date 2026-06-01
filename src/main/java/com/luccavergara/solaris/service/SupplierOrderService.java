package com.luccavergara.solaris.service;

import com.luccavergara.solaris.dto.SupplierOrderItemResponse;
import com.luccavergara.solaris.dto.SupplierOrderRequest;
import com.luccavergara.solaris.dto.SupplierOrderResponse;
import com.luccavergara.solaris.entity.Product;
import com.luccavergara.solaris.entity.Supplier;
import com.luccavergara.solaris.entity.SupplierOrder;
import com.luccavergara.solaris.entity.SupplierOrderItem;
import com.luccavergara.solaris.entity.SupplierOrderStatus;
import com.luccavergara.solaris.entity.User;
import com.luccavergara.solaris.exception.ResourceNotFoundException;
import com.luccavergara.solaris.repository.ProductRepository;
import com.luccavergara.solaris.repository.SupplierOrderRepository;
import com.luccavergara.solaris.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SupplierOrderService {

    private final SupplierOrderRepository supplierOrderRepository;
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;
    private final AuthenticatedUserService authenticatedUserService;

    @Transactional
    public SupplierOrderResponse createSupplierOrder(SupplierOrderRequest request) {
        User currentUser = authenticatedUserService.getCurrentUser();

        Supplier supplier = supplierRepository.findByIdAndUser(request.getSupplierId(), currentUser)
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
                    Product product = productRepository.findByIdAndUser(itemRequest.getProductId(), currentUser)
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

        return mapToResponse(supplierOrderRepository.save(supplierOrder));
    }

    public List<SupplierOrderResponse> getAllSupplierOrders() {
        User currentUser = authenticatedUserService.getCurrentUser();

        return supplierOrderRepository.findAllByUser(currentUser)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public SupplierOrderResponse getSupplierOrderById(Long id) {
        User currentUser = authenticatedUserService.getCurrentUser();

        SupplierOrder supplierOrder = supplierOrderRepository.findByIdAndUser(id, currentUser)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier order not found"));

        return mapToResponse(supplierOrder);
    }

    @Transactional
    public SupplierOrderResponse markAsSent(Long id) {
        User currentUser = authenticatedUserService.getCurrentUser();

        SupplierOrder supplierOrder = supplierOrderRepository.findByIdAndUser(id, currentUser)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier order not found"));

        supplierOrder.setStatus(SupplierOrderStatus.SENT);
        supplierOrder.setUpdatedAt(LocalDateTime.now());

        return mapToResponse(supplierOrderRepository.save(supplierOrder));
    }

    @Transactional
    public SupplierOrderResponse markAsCompleted(Long id) {
        User currentUser = authenticatedUserService.getCurrentUser();

        SupplierOrder supplierOrder = supplierOrderRepository.findByIdAndUser(id, currentUser)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier order not found"));

        supplierOrder.setStatus(SupplierOrderStatus.COMPLETED);
        supplierOrder.setUpdatedAt(LocalDateTime.now());

        return mapToResponse(supplierOrderRepository.save(supplierOrder));
    }

    @Transactional
    public SupplierOrderResponse cancelSupplierOrder(Long id) {
        User currentUser = authenticatedUserService.getCurrentUser();

        SupplierOrder supplierOrder = supplierOrderRepository.findByIdAndUser(id, currentUser)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier order not found"));

        supplierOrder.setStatus(SupplierOrderStatus.CANCELLED);
        supplierOrder.setUpdatedAt(LocalDateTime.now());

        return mapToResponse(supplierOrderRepository.save(supplierOrder));
    }

    public void deleteSupplierOrder(Long id) {
        User currentUser = authenticatedUserService.getCurrentUser();

        SupplierOrder supplierOrder = supplierOrderRepository.findByIdAndUser(id, currentUser)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier order not found"));

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
                .productSku(item.getProduct().getSku())
                .quantity(item.getQuantity())
                .build();
    }
}