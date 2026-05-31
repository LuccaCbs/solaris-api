package com.luccavergara.solaris.service;

import com.luccavergara.solaris.dto.DashboardMonthlySalesResponse;
import com.luccavergara.solaris.dto.DashboardResponse;
import com.luccavergara.solaris.dto.DashboardSupplierOrdersResponse;
import com.luccavergara.solaris.entity.Sale;
import com.luccavergara.solaris.entity.SupplierOrder;
import com.luccavergara.solaris.entity.SupplierOrderStatus;
import com.luccavergara.solaris.repository.ProductRepository;
import com.luccavergara.solaris.repository.SaleRepository;
import com.luccavergara.solaris.repository.SupplierOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final SaleRepository saleRepository;
    private final ProductRepository productRepository;
    private final SupplierOrderRepository supplierOrderRepository;
    private final SystemSettingsService systemSettingsService;

    public DashboardResponse getDashboard() {
        List<Sale> sales = saleRepository.findAll();
        List<SupplierOrder> supplierOrders = supplierOrderRepository.findAll();

        LocalDate today = LocalDate.now();

        List<Sale> todaySales = sales.stream()
                .filter(sale -> sale.getCreatedAt().toLocalDate().isEqual(today))
                .toList();

        BigDecimal todaySalesAmount = todaySales.stream()
                .map(Sale::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return DashboardResponse.builder()
                .todaySalesCount(todaySales.size())
                .todaySalesAmount(todaySalesAmount)
                .lowStockProductsCount(countLowStockProducts())
                .supplierOrders(buildSupplierOrdersResponse(supplierOrders))
                .monthlySales(buildMonthlySales(sales))
                .build();
    }

    private Integer countLowStockProducts() {
        Integer globalLowStockThreshold = systemSettingsService
                .getOrCreateSettings()
                .getGlobalLowStockThreshold();

        return (int) productRepository.findAll()
                .stream()
                .filter(product -> {
                    Integer effectiveThreshold = product.getLowStockThreshold() != null
                            ? product.getLowStockThreshold()
                            : globalLowStockThreshold;

                    return product.getStockQuantity() != null
                            && product.getStockQuantity() <= effectiveThreshold;
                })
                .count();
    }

    private DashboardSupplierOrdersResponse buildSupplierOrdersResponse(
            List<SupplierOrder> supplierOrders
    ) {
        return DashboardSupplierOrdersResponse.builder()
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

    private List<DashboardMonthlySalesResponse> buildMonthlySales(List<Sale> sales) {
        YearMonth currentMonth = YearMonth.now();

        return currentMonth
                .atDay(1)
                .datesUntil(currentMonth.plusMonths(1).atDay(1))
                .map(date -> {
                    List<Sale> salesByDate = sales.stream()
                            .filter(sale -> sale.getCreatedAt().toLocalDate().isEqual(date))
                            .toList();

                    BigDecimal totalAmount = salesByDate.stream()
                            .map(Sale::getTotalAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    return DashboardMonthlySalesResponse.builder()
                            .date(date)
                            .salesCount(salesByDate.size())
                            .totalAmount(totalAmount)
                            .build();
                })
                .toList();
    }
}