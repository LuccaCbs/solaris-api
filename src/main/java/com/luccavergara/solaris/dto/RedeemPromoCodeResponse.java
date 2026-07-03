package com.luccavergara.solaris.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RedeemPromoCodeResponse {

    private String message;
    private PromoCodeRedemptionResponse redemption;
    private OrganizationEntitlementsResponse entitlements;
}
