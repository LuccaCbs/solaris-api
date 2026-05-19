package com.luccavergara.solaris.service;

import com.luccavergara.solaris.dto.DashboardSummaryResponse;
import com.luccavergara.solaris.repository.CategoryRepository;
import com.luccavergara.solaris.repository.ProductRepository;
import com.luccavergara.solaris.repository.StockMovementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final StockMovementRepository stockMovementRepository;

    public DashboardSummaryResponse getSummary() {
        var products = productRepository.findAll();

        int totalStockUnits = products.stream()
                .mapToInt(product -> product.getStockQuantity() != null ? product.getStockQuantity() : 0)
                .sum();

        long lowStockProducts = products.stream()
                .filter(product -> product.getStockQuantity() != null && product.getStockQuantity() <= 5)
                .count();

        return DashboardSummaryResponse.builder()
                .totalProducts(products.size())
                .totalCategories(categoryRepository.count())
                .totalStockUnits(totalStockUnits)
                .lowStockProducts(lowStockProducts)
                .totalStockMovements(stockMovementRepository.count())
                .build();
    }
}