package com.luccavergara.solaris.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardSalesComparison {

    private Integer yesterdaySalesCount;
    private BigDecimal yesterdaySalesAmount;

    private Integer currentMonthSalesCount;
    private BigDecimal currentMonthSalesAmount;

    private Integer previousMonthSalesCount;
    private BigDecimal previousMonthSalesAmount;
}
