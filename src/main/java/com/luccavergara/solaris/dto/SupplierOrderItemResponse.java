package com.luccavergara.solaris.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupplierOrderItemResponse {

    private Long id;
    private Long productId;
    private String productName;
    private String productBarcode;
    private Integer quantity;
}