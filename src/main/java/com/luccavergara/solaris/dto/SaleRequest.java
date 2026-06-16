package com.luccavergara.solaris.dto;

import com.luccavergara.solaris.entity.PaymentMethod;
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
public class SaleRequest {

    @NotNull
    private PaymentMethod paymentMethod;

    @NotEmpty
    @Valid
    private List<SaleItemRequest> items;
}