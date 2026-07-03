package com.luccavergara.solaris.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "fiscal_documents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FiscalDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id")
    private Store store;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sale_id", unique = true)
    private Sale sale;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_comprobante", nullable = false)
    private TipoComprobante tipoComprobante;

    @Column(name = "punto_venta", nullable = false)
    private Integer puntoVenta;

    @Column(name = "numero_comprobante", nullable = false)
    private Long numeroComprobante;

    @Column(length = 14)
    private String cae;

    @Column(name = "cae_vencimiento")
    private LocalDate caeVencimiento;

    @Column(name = "importe_neto", nullable = false)
    private BigDecimal importeNeto;

    @Column(name = "importe_iva", nullable = false)
    private BigDecimal importeIva;

    @Column(name = "importe_total", nullable = false)
    private BigDecimal importeTotal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FiscalDocumentStatus status;

    @Column(name = "afip_raw_json", columnDefinition = "TEXT")
    private String afipRawJson;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "pdf_url", length = 1024)
    private String pdfUrl;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
