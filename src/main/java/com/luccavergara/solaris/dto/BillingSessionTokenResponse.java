package com.luccavergara.solaris.dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class BillingSessionTokenResponse {
    String billingToken;
    LocalDateTime expiresAt;
}
