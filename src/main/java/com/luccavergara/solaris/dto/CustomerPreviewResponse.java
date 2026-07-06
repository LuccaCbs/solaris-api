package com.luccavergara.solaris.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerPreviewResponse {

    private CustomerResponse customer;
    private Long totalInvoicedDocuments;
    private List<FiscalDocumentResponse> invoicedDocuments;
}
