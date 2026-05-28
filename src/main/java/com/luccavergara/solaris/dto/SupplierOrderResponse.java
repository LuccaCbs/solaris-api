package com.luccavergara.solaris.dto;

import com.luccavergara.solaris.entity.SupplierOrderStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupplierOrderResponse {

    private Long id;

    private Long supplierId;
    private String supplierName;
    private String supplierPhone;

    private SupplierOrderStatus status;

    private String messagePreview;

    private List<SupplierOrderItemResponse> items;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}