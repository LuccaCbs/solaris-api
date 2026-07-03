package com.luccavergara.solaris.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardSupplierOrdersResponse {

    private Integer draft;
    private Integer sent;
    private Integer completed;
    private Integer cancelled;
}