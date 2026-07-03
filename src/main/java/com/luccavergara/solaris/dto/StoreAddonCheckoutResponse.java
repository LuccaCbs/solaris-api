package com.luccavergara.solaris.dto;

import com.luccavergara.solaris.entity.BillingProvider;
import com.luccavergara.solaris.entity.PaymentMethodType;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

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
    private String providerDisplayName;
    private List<PaymentMethodType> supportedPaymentMethods;
    private Integer quantity;
    private String currency;
    private BigDecimal unitPrice;
    private BigDecimal unitPriceArs;
    private Boolean mockPurchaseAvailable;
    private Long checkoutId;
    private String preferenceId;
}
