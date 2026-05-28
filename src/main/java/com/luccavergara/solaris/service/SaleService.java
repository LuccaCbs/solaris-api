package com.luccavergara.solaris.service;

import com.luccavergara.solaris.dto.DailySalesSummaryResponse;
import com.luccavergara.solaris.dto.SaleItemResponse;
import com.luccavergara.solaris.dto.SaleRequest;
import com.luccavergara.solaris.dto.SaleResponse;
import com.luccavergara.solaris.entity.CashRegisterSession;
import com.luccavergara.solaris.entity.CashRegisterStatus;
import com.luccavergara.solaris.entity.PaymentMethod;
import com.luccavergara.solaris.entity.Product;
import com.luccavergara.solaris.entity.Sale;
import com.luccavergara.solaris.entity.SaleItem;
import com.luccavergara.solaris.entity.StockMovement;
import com.luccavergara.solaris.entity.StockMovementType;
import com.luccavergara.solaris.exception.ResourceNotFoundException;
import com.luccavergara.solaris.repository.CashRegisterSessionRepository;
import com.luccavergara.solaris.repository.ProductRepository;
import com.luccavergara.solaris.repository.SaleRepository;
import com.luccavergara.solaris.repository.StockMovementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SaleService {

    private final SaleRepository saleRepository;
    private final ProductRepository productRepository;
    private final StockMovementRepository stockMovementRepository;
    private final CashRegisterSessionRepository cashRegisterSessionRepository;

    @Transactional
    public SaleResponse createSale(SaleRequest request) {
        CashRegisterSession cashRegisterSession = cashRegisterSessionRepository
                .findFirstByStatusOrderByOpenedAtDesc(CashRegisterStatus.OPEN)
                .orElseThrow(() -> new IllegalStateException("There is no open cash register session"));

        Sale sale = Sale.builder()
                .cashRegisterSession(cashRegisterSession)
                .paymentMethod(request.getPaymentMethod())
                .totalAmount(BigDecimal.ZERO)
                .createdAt(LocalDateTime.now())
                .items(new ArrayList<>())
                .build();

        BigDecimal totalAmount = BigDecimal.ZERO;

        for (var itemRequest : request.getItems()) {
            Product product = productRepository.findById(itemRequest.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

            int previousStock = product.getStockQuantity();
            int quantity = itemRequest.getQuantity();

            if (previousStock < quantity) {
                throw new IllegalArgumentException("Insufficient stock for product: " + product.getName());
            }

            int currentStock = previousStock - quantity;

            product.setStockQuantity(currentStock);
            productRepository.save(product);

            BigDecimal unitPrice = product.getPrice();
            BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(quantity));

            SaleItem saleItem = SaleItem.builder()
                    .sale(sale)
                    .product(product)
                    .quantity(quantity)
                    .unitPrice(unitPrice)
                    .subtotal(subtotal)
                    .build();

            sale.getItems().add(saleItem);

            StockMovement movement = StockMovement.builder()
                    .product(product)
                    .type(StockMovementType.OUT)
                    .quantity(quantity)
                    .previousStock(previousStock)
                    .currentStock(currentStock)
                    .reason("Sale transaction")
                    .createdAt(LocalDateTime.now())
                    .build();

            stockMovementRepository.save(movement);

            totalAmount = totalAmount.add(subtotal);
        }

        sale.setTotalAmount(totalAmount);

        return mapToResponse(saleRepository.save(sale));
    }

    public List<SaleResponse> getAllSales() {
        return saleRepository.findAll()
                .stream()
                .sorted(Comparator.comparing(Sale::getCreatedAt).reversed())
                .map(this::mapToResponse)
                .toList();
    }

    public SaleResponse getSaleById(Long id) {
        Sale sale = saleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sale not found"));

        return mapToResponse(sale);
    }

    public DailySalesSummaryResponse getDailySummary(LocalDate date) {
        LocalDate targetDate = date != null ? date : LocalDate.now();

        LocalDateTime startOfDay = targetDate.atStartOfDay();
        LocalDateTime endOfDay = targetDate.atTime(LocalTime.MAX);

        List<Sale> sales = saleRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(
                startOfDay,
                endOfDay
        );

        BigDecimal cashTotal = calculateTotalByPaymentMethod(sales, PaymentMethod.CASH);
        BigDecimal debitCardTotal = calculateTotalByPaymentMethod(sales, PaymentMethod.DEBIT_CARD);
        BigDecimal creditCardTotal = calculateTotalByPaymentMethod(sales, PaymentMethod.CREDIT_CARD);
        BigDecimal transferTotal = calculateTotalByPaymentMethod(sales, PaymentMethod.TRANSFER);
        BigDecimal otherTotal = calculateTotalByPaymentMethod(sales, PaymentMethod.OTHER);

        BigDecimal totalSales = sales.stream()
                .map(Sale::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return DailySalesSummaryResponse.builder()
                .date(targetDate)
                .salesCount(sales.size())
                .totalSales(totalSales)
                .cashTotal(cashTotal)
                .debitCardTotal(debitCardTotal)
                .creditCardTotal(creditCardTotal)
                .transferTotal(transferTotal)
                .otherTotal(otherTotal)
                .build();
    }

    private BigDecimal calculateTotalByPaymentMethod(
            List<Sale> sales,
            PaymentMethod paymentMethod
    ) {
        return sales.stream()
                .filter(sale -> sale.getPaymentMethod() == paymentMethod)
                .map(Sale::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private SaleResponse mapToResponse(Sale sale) {
        return SaleResponse.builder()
                .id(sale.getId())
                .cashRegisterSessionId(
                        sale.getCashRegisterSession() != null
                                ? sale.getCashRegisterSession().getId()
                                : null
                )
                .paymentMethod(sale.getPaymentMethod())
                .totalAmount(sale.getTotalAmount())
                .createdAt(sale.getCreatedAt())
                .items(
                        sale.getItems()
                                .stream()
                                .map(this::mapItemToResponse)
                                .toList()
                )
                .build();
    }

    private SaleItemResponse mapItemToResponse(SaleItem item) {
        return SaleItemResponse.builder()
                .id(item.getId())
                .productId(item.getProduct().getId())
                .productName(item.getProduct().getName())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .subtotal(item.getSubtotal())
                .build();
    }
}