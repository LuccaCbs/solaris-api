package com.luccavergara.solaris.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardSummaryResponse {

    private long totalProducts;
    private long totalCategories;
    private int totalStockUnits;
    private long lowStockProducts;
    private long totalStockMovements;
}