package com.luccavergara.solaris.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupplierPreviewResponse {

    private SupplierResponse supplier;
    private Long totalOrders;
    private List<SupplierOrderResponse> recentOrders;
}
