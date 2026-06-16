package com.luccavergara.solaris.dto;

import com.luccavergara.solaris.entity.SaleItemType;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SaleItemResponse {

    private Long id;
    private SaleItemType type;

    private Long productId;
    private String productName;

    private String customName;
    private String unitLabel;

    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal subtotal;
}