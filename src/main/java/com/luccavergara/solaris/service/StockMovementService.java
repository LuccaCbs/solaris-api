package com.luccavergara.solaris.service;

import com.luccavergara.solaris.dto.BulkStockMovementRequest;
import com.luccavergara.solaris.dto.StockMovementRequest;
import com.luccavergara.solaris.dto.StockMovementResponse;
import com.luccavergara.solaris.entity.Product;
import com.luccavergara.solaris.entity.StockMovement;
import com.luccavergara.solaris.entity.StockMovementType;
import com.luccavergara.solaris.entity.User;
import com.luccavergara.solaris.exception.ResourceNotFoundException;
import com.luccavergara.solaris.repository.ProductRepository;
import com.luccavergara.solaris.repository.StockMovementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StockMovementService {

    private final StockMovementRepository stockMovementRepository;
    private final ProductRepository productRepository;
    private final AuthenticatedUserService authenticatedUserService;
    private final TenantQueryService tenantQueryService;
    private final TenantScopeService tenantScopeService;

    public StockMovementResponse createMovement(StockMovementRequest request) {
        return persistMovement(
                request.getProductId(),
                request.getType(),
                request.getQuantity(),
                request.getReason()
        );
    }

    @Transactional
    public List<StockMovementResponse> createMovements(BulkStockMovementRequest request) {
        return request.getItems().stream()
                .map(item -> persistMovement(
                        item.getProductId(),
                        StockMovementType.IN,
                        item.getQuantity(),
                        request.getReason()
                ))
                .toList();
    }

    private StockMovementResponse persistMovement(
            Long productId,
            StockMovementType type,
            Integer quantity,
            String reason
    ) {
        User currentUser = authenticatedUserService.getCurrentUser();

        Product product = tenantQueryService.findProductById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        int previousStock = product.getStockQuantity();

        applyStockMovement(product, StockMovementRequest.builder()
                .productId(productId)
                .type(type)
                .quantity(quantity)
                .reason(reason)
                .build());

        int currentStock = product.getStockQuantity();

        productRepository.save(product);

        StockMovement movement = StockMovement.builder()
                .product(product)
                .user(currentUser)
                .type(type)
                .quantity(quantity)
                .previousStock(previousStock)
                .currentStock(currentStock)
                .reason(reason)
                .createdAt(LocalDateTime.now())
                .build();

        tenantScopeService.getOrganizationReference(currentUser)
                .ifPresent(organization -> {
                    movement.setOrganization(organization);
                    movement.setCreatedBy(currentUser);
                });

        return mapToResponse(stockMovementRepository.save(movement));
    }

    public List<StockMovementResponse> getAllMovements() {
        return tenantQueryService.findAllStockMovements()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<StockMovementResponse> getMovementsByProduct(Long productId) {
        tenantQueryService.findProductById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        return tenantQueryService.findStockMovementsByProductId(productId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private void applyStockMovement(Product product, StockMovementRequest request) {
        int currentStock = product.getStockQuantity();
        int quantity = request.getQuantity();

        if (request.getType() == StockMovementType.IN) {
            product.setStockQuantity(currentStock + quantity);
            return;
        }

        if (request.getType() == StockMovementType.OUT) {
            if (currentStock < quantity) {
                throw new IllegalArgumentException("Insufficient stock for this operation");
            }
            product.setStockQuantity(currentStock - quantity);
            return;
        }

        if (request.getType() == StockMovementType.ADJUSTMENT) {
            product.setStockQuantity(quantity);
        }
    }

    private StockMovementResponse mapToResponse(StockMovement movement) {
        return StockMovementResponse.builder()
                .id(movement.getId())
                .productId(movement.getProduct().getId())
                .productName(movement.getProduct().getName())
                .type(movement.getType())
                .quantity(movement.getQuantity())
                .previousStock(movement.getPreviousStock())
                .currentStock(movement.getCurrentStock())
                .reason(movement.getReason())
                .createdAt(movement.getCreatedAt())
                .build();
    }
}
