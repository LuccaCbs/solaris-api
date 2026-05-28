package com.luccavergara.solaris.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CashRegisterAuthorizationRequest {

    @NotBlank
    private String adminPassword;
}