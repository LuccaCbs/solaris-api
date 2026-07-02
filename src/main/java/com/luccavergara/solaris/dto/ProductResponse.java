package com.luccavergara.solaris.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductResponse {

    private Long id;
    private String name;
    private String description;
    private String sku;
    private BigDecimal price;
    private Integer stockQuantity;
    private LocalDateTime createdAt;
    private Long categoryId;
    private String categoryName;
    private Integer lowStockThreshold;
    private Integer effectiveLowStockThreshold;
    private Boolean lowStock;
    private Boolean active;
}