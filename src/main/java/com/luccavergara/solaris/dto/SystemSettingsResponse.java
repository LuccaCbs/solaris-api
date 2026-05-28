package com.luccavergara.solaris.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemSettingsResponse {

    private Long id;
    private Integer globalLowStockThreshold;
    private Boolean hasAdminAccessPassword;
    private LocalDateTime updatedAt;
    private String businessTimezone;
    private LocalTime cashRegisterAutoCloseTime;
    private Boolean whatsappEnabled;
}