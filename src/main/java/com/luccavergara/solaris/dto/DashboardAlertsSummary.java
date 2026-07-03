package com.luccavergara.solaris.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardAlertsSummary {

    private Integer draftSupplierOrders;
    private Integer sentSupplierOrders;
    private Integer rejectedFiscalDocumentsToday;
    private Integer inactiveProducts;
}
