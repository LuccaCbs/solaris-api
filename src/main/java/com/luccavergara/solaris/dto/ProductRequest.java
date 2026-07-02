package com.luccavergara.solaris.dto;

import com.luccavergara.solaris.entity.ProductIvaRate;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductRequest {

    @NotBlank
    private String name;

    private String description;

    private String sku;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal price;

    @NotNull
    @Min(0)
    private Integer stockQuantity;

    private Integer lowStockThreshold;

    private Long categoryId;

    private ProductIvaRate ivaRate;
}