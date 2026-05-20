package com.luccavergara.solaris.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemSettingsResponse {

    private Long id;
    private Integer globalLowStockThreshold;
    private LocalDateTime updatedAt;
}