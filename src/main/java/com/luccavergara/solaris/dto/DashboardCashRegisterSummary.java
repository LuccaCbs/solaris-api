package com.luccavergara.solaris.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardCashRegisterSummary {

    private Boolean open;
    private Long sessionId;
    private LocalDateTime openedAt;
    private String openedBy;
}
