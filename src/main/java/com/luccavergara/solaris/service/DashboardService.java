package com.luccavergara.solaris.service;

import com.luccavergara.solaris.dto.*;
import com.luccavergara.solaris.entity.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final TenantQueryService tenantQueryService;
    private final SystemSettingsService systemSettingsService;

    @Transactional(readOnly = true)
    public DashboardResponse getDashboard() {
        List<Sale> sales = tenantQueryService.findAllSales();
        List<SupplierOrder> supplierOrders = tenantQueryService.findAllSupplierOrders();
        List<FiscalDocument> fiscalDocuments = tenantQueryService.findAllFiscalDocuments();
        List<Product> products = tenantQueryService.findAllProducts();

        LocalDate today = LocalDate.now();
        YearMonth currentMonth = YearMonth.now();
        YearMonth previousMonth = currentMonth.minusMonths(1);

        List<Sale> todaySales = filterSalesByDate(sales, today);
        List<Sale> yesterdaySales = filterSalesByDate(sales, today.minusDays(1));
        List<Sale> currentMonthSales = filterSalesByMonth(sales, currentMonth);
        List<Sale> previousMonthSales = filterSalesByMonth(sales, previousMonth);

        BigDecimal todaySalesAmount = sumSaleAmounts(todaySales);

        return DashboardResponse.builder()
                .todaySalesCount(todaySales.size())
                .todaySalesAmount(todaySalesAmount)
                .lowStockProductsCount(countLowStockProducts(products))
                .supplierOrders(buildSupplierOrdersResponse(supplierOrders))
                .monthlySales(buildMonthlySales(currentMonthSales, currentMonth))
                .cashRegister(buildCashRegisterSummary())
                .alerts(buildAlertsSummary(supplierOrders, fiscalDocuments, products, today))
                .comparison(buildSalesComparison(
                        todaySales,
                        yesterdaySales,
                        currentMonthSales,
                        previousMonthSales
                ))
                .topProducts(buildTopProducts(currentMonthSales))
                .recentSales(buildRecentSales(sales))
                .build();
    }

    private List<Sale> filterSalesByDate(List<Sale> sales, LocalDate date) {
        return sales.stream()
                .filter(sale -> sale.getCreatedAt().toLocalDate().isEqual(date))
                .toList();
    }

    private List<Sale> filterSalesByMonth(List<Sale> sales, YearMonth month) {
        return sales.stream()
                .filter(sale -> YearMonth.from(sale.getCreatedAt()).equals(month))
                .toList();
    }

    private BigDecimal sumSaleAmounts(List<Sale> sales) {
        return sales.stream()
                .map(Sale::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Integer countLowStockProducts(List<Product> products) {
        Integer globalLowStockThreshold = systemSettingsService
                .getOrCreateSettings()
                .getGlobalLowStockThreshold();

        return (int) products.stream()
                .filter(product -> product.getActive() == null || product.getActive())
                .filter(product -> isLowStock(product, globalLowStockThreshold))
                .count();
    }

    private boolean isLowStock(Product product, Integer globalLowStockThreshold) {
        Integer effectiveThreshold = product.getLowStockThreshold() != null
                ? product.getLowStockThreshold()
                : globalLowStockThreshold;

        return product.getStockQuantity() != null
                && product.getStockQuantity() <= effectiveThreshold;
    }

    private DashboardSupplierOrdersResponse buildSupplierOrdersResponse(
            List<SupplierOrder> supplierOrders
    ) {
        return DashboardSupplierOrdersResponse.builder()
                .draft(countSupplierOrdersByStatus(supplierOrders, SupplierOrderStatus.DRAFT))
                .sent(countSupplierOrdersByStatus(supplierOrders, SupplierOrderStatus.SENT))
                .completed(countSupplierOrdersByStatus(supplierOrders, SupplierOrderStatus.COMPLETED))
                .cancelled(countSupplierOrdersByStatus(supplierOrders, SupplierOrderStatus.CANCELLED))
                .build();
    }

    private Integer countSupplierOrdersByStatus(
            List<SupplierOrder> supplierOrders,
            SupplierOrderStatus status
    ) {
        return (int) supplierOrders.stream()
                .filter(order -> order.getStatus() == status)
                .count();
    }

    private List<DashboardMonthlySalesResponse> buildMonthlySales(
            List<Sale> monthSales,
            YearMonth currentMonth
    ) {
        return currentMonth
                .atDay(1)
                .datesUntil(currentMonth.plusMonths(1).atDay(1))
                .map(date -> {
                    List<Sale> salesByDate = filterSalesByDate(monthSales, date);

                    return DashboardMonthlySalesResponse.builder()
                            .date(date)
                            .salesCount(salesByDate.size())
                            .totalAmount(sumSaleAmounts(salesByDate))
                            .build();
                })
                .toList();
    }

    private DashboardCashRegisterSummary buildCashRegisterSummary() {
        return tenantQueryService.findOpenCashRegisterSession()
                .map(session -> DashboardCashRegisterSummary.builder()
                        .open(true)
                        .sessionId(session.getId())
                        .openedAt(session.getOpenedAt())
                        .openedBy(session.getOpenedBy())
                        .build())
                .orElseGet(() -> DashboardCashRegisterSummary.builder()
                        .open(false)
                        .build());
    }

    private DashboardAlertsSummary buildAlertsSummary(
            List<SupplierOrder> supplierOrders,
            List<FiscalDocument> fiscalDocuments,
            List<Product> products,
            LocalDate today
    ) {
        int rejectedToday = (int) fiscalDocuments.stream()
                .filter(document -> document.getStatus() == FiscalDocumentStatus.REJECTED)
                .filter(document -> document.getCreatedAt().toLocalDate().isEqual(today))
                .count();

        int inactiveProducts = (int) products.stream()
                .filter(product -> Boolean.FALSE.equals(product.getActive()))
                .count();

        return DashboardAlertsSummary.builder()
                .draftSupplierOrders(countSupplierOrdersByStatus(supplierOrders, SupplierOrderStatus.DRAFT))
                .sentSupplierOrders(countSupplierOrdersByStatus(supplierOrders, SupplierOrderStatus.SENT))
                .rejectedFiscalDocumentsToday(rejectedToday)
                .inactiveProducts(inactiveProducts)
                .build();
    }

    private DashboardSalesComparison buildSalesComparison(
            List<Sale> todaySales,
            List<Sale> yesterdaySales,
            List<Sale> currentMonthSales,
            List<Sale> previousMonthSales
    ) {
        return DashboardSalesComparison.builder()
                .yesterdaySalesCount(yesterdaySales.size())
                .yesterdaySalesAmount(sumSaleAmounts(yesterdaySales))
                .currentMonthSalesCount(currentMonthSales.size())
                .currentMonthSalesAmount(sumSaleAmounts(currentMonthSales))
                .previousMonthSalesCount(previousMonthSales.size())
                .previousMonthSalesAmount(sumSaleAmounts(previousMonthSales))
                .build();
    }

    private List<DashboardTopProductItem> buildTopProducts(List<Sale> monthSales) {
        Map<Long, TopProductAccumulator> accumulators = new HashMap<>();

        for (Sale sale : monthSales) {
            if (sale.getItems() == null) {
                continue;
            }

            for (SaleItem item : sale.getItems()) {
                if (item.getType() != SaleItemType.PRODUCT || item.getProduct() == null) {
                    continue;
                }

                Long productId = item.getProduct().getId();
                TopProductAccumulator accumulator = accumulators.computeIfAbsent(
                        productId,
                        ignored -> new TopProductAccumulator(
                                productId,
                                item.getProduct().getName()
                        )
                );

                accumulator.quantitySold += item.getQuantity();
                accumulator.totalAmount = accumulator.totalAmount.add(item.getSubtotal());
            }
        }

        return accumulators.values().stream()
                .sorted(Comparator.comparingInt((TopProductAccumulator item) -> item.quantitySold).reversed())
                .limit(5)
                .map(accumulator -> DashboardTopProductItem.builder()
                        .productId(accumulator.productId)
                        .productName(accumulator.productName)
                        .quantitySold(accumulator.quantitySold)
                        .totalAmount(accumulator.totalAmount)
                        .build())
                .toList();
    }

    private List<DashboardRecentSaleItem> buildRecentSales(List<Sale> sales) {
        return sales.stream()
                .limit(8)
                .map(sale -> DashboardRecentSaleItem.builder()
                        .id(sale.getId())
                        .totalAmount(sale.getTotalAmount())
                        .paymentMethod(sale.getPaymentMethod())
                        .createdAt(sale.getCreatedAt())
                        .itemCount(sale.getItems() != null ? sale.getItems().size() : 0)
                        .build())
                .collect(Collectors.toList());
    }

    private static class TopProductAccumulator {
        private final Long productId;
        private final String productName;
        private int quantitySold;
        private BigDecimal totalAmount = BigDecimal.ZERO;

        private TopProductAccumulator(Long productId, String productName) {
            this.productId = productId;
            this.productName = productName;
        }
    }
}
