package com.luccavergara.solaris.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardResponse {

    private Integer todaySalesCount;
    private BigDecimal todaySalesAmount;

    private Integer lowStockProductsCount;

    private DashboardSupplierOrdersResponse supplierOrders;

    private List<DashboardMonthlySalesResponse> monthlySales;

    private DashboardCashRegisterSummary cashRegister;

    private DashboardAlertsSummary alerts;

    private DashboardSalesComparison comparison;

    private List<DashboardTopProductItem> topProducts;

    private List<DashboardRecentSaleItem> recentSales;
}
