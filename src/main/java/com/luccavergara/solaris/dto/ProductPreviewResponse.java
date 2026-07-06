package com.luccavergara.solaris.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductPreviewResponse {

    private ProductResponse product;
    private Integer salesQuantityThisMonth;
    private BigDecimal salesRevenueThisMonth;
    private Long supplierOrderAppearances;
    private Long restockCount;
    private List<StockMovementResponse> recentStockMovements;
}
