package com.luccavergara.solaris.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailySalesSummaryResponse {

    private LocalDate date;
    private long salesCount;
    private BigDecimal totalSales;
    private BigDecimal cashTotal;
    private BigDecimal debitCardTotal;
    private BigDecimal creditCardTotal;
    private BigDecimal transferTotal;
    private BigDecimal otherTotal;
}