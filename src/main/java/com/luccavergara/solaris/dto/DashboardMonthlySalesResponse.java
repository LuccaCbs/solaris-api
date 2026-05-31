package com.luccavergara.solaris.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardMonthlySalesResponse {

    private LocalDate date;
    private Integer salesCount;
    private BigDecimal totalAmount;
}