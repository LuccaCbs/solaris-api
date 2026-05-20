package com.luccavergara.solaris.service;

import com.luccavergara.solaris.dto.StockMovementRequest;
import com.luccavergara.solaris.dto.StockMovementResponse;
import com.luccavergara.solaris.entity.Product;
import com.luccavergara.solaris.entity.StockMovement;
import com.luccavergara.solaris.entity.StockMovementType;
import com.luccavergara.solaris.exception.ResourceNotFoundException;
import com.luccavergara.solaris.repository.ProductRepository;
import com.luccavergara.solaris.repository.StockMovementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StockMovementService {

    private final StockMovementRepository stockMovementRepository;
    private final ProductRepository productRepository;

    public StockMovementResponse createMovement(StockMovementRequest request) {
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        int previousStock = product.getStockQuantity();

        applyStockMovement(product, request);

        int currentStock = product.getStockQuantity();

        productRepository.save(product);

        StockMovement movement = StockMovement.builder()
                .product(product)
                .type(request.getType())
                .quantity(request.getQuantity())
                .previousStock(previousStock)
                .currentStock(currentStock)
                .reason(request.getReason())
                .createdAt(LocalDateTime.now())
                .build();

        return mapToResponse(stockMovementRepository.save(movement));
    }

    public List<StockMovementResponse> getAllMovements() {
        return stockMovementRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<StockMovementResponse> getMovementsByProduct(Long productId) {
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Product not found");
        }

        return stockMovementRepository.findByProductIdOrderByCreatedAtDesc(productId)
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