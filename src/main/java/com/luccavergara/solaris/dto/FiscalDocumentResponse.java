package com.luccavergara.solaris.dto;

import com.luccavergara.solaris.entity.FiscalDocumentStatus;
import com.luccavergara.solaris.entity.TipoComprobante;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class FiscalDocumentResponse {

    private Long id;
    private Long organizationId;
    private Long storeId;
    private Long saleId;
    private Long customerId;
    private String customerRazonSocial;
    private TipoComprobante tipoComprobante;
    private Integer puntoVenta;
    private Long numeroComprobante;
    private String cae;
    private LocalDate caeVencimiento;
    private BigDecimal importeNeto;
    private BigDecimal importeIva;
    private BigDecimal importeTotal;
    private FiscalDocumentStatus status;
    private String pdfUrl;
    private LocalDateTime createdAt;
}
