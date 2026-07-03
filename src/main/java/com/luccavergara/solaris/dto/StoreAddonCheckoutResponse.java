package com.luccavergara.solaris.dto;

import com.luccavergara.solaris.entity.BillingProvider;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreAddonCheckoutResponse {

    private String status;
    private String message;
    private String checkoutUrl;
    private BillingProvider provider;
    private Integer quantity;
    private BigDecimal unitPriceArs;
    private Boolean mockPurchaseAvailable;
}
