package com.luccavergara.solaris.dto;

import com.luccavergara.solaris.entity.ProductIvaRate;
import com.luccavergara.solaris.entity.BarcodeFormat;
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

    private String barcode;

    private BarcodeFormat barcodeFormat;

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