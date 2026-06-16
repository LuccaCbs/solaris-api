package com.luccavergara.solaris.dto;

import com.luccavergara.solaris.entity.SaleItemType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SaleItemRequest {

    @NotNull
    private SaleItemType type;

    private Long productId;

    private String customName;

    @NotNull
    @Min(1)
    private Integer quantity;

    private String unitLabel;

    @DecimalMin("0.0")
    private BigDecimal unitPrice;
}