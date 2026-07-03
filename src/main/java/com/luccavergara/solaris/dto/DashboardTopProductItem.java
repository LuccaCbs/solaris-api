package com.luccavergara.solaris.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardTopProductItem {

    private Long productId;
    private String productName;
    private Integer quantitySold;
    private BigDecimal totalAmount;
}
