package com.luccavergara.solaris.dto;

import com.luccavergara.solaris.entity.PaymentMethod;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SaleResponse {

    private Long id;
    private PaymentMethod paymentMethod;
    private BigDecimal totalAmount;
    private LocalDateTime createdAt;
    private Long cashRegisterSessionId;
    private List<SaleItemResponse> items;
    private Boolean invoiced;
    private Long fiscalDocumentId;
}