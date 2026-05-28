package com.luccavergara.solaris.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupplierOrderRequest {

    @NotNull
    private Long supplierId;

    @Valid
    @NotEmpty
    private List<SupplierOrderItemRequest> items;
}