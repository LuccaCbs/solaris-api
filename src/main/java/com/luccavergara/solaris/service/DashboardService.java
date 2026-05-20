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
    private final SystemSettingsService systemSettingsService;
    public DashboardSummaryResponse getSummary() {

        var products = productRepository.findAll();

        int totalStockUnits = products.stream()
                .mapToInt(product -> product.getStockQuantity() != null ? product.getStockQuantity() : 0)
                .sum();

        Integer globalLowStockThreshold = systemSettingsService
                .getOrCreateSettings()
                .getGlobalLowStockThreshold();

        long lowStockProducts = products.stream()
                .filter(product -> {
                    Integer effectiveThreshold = product.getLowStockThreshold() != null
                            ? product.getLowStockThreshold()
                            : globalLowStockThreshold;

                    return product.getStockQuantity() != null
                            && product.getStockQuantity() <= effectiveThreshold;
                })
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