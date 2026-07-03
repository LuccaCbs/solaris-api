package com.luccavergara.solaris.dto;

import com.luccavergara.solaris.entity.ProductIvaRate;
import com.luccavergara.solaris.entity.BarcodeFormat;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductUpdateRequest {

    @NotBlank
    private String name;

    private String description;

    private String barcode;

    private BarcodeFormat barcodeFormat;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal price;

    private Long categoryId;

    private Integer lowStockThreshold;

    private ProductIvaRate ivaRate;
}