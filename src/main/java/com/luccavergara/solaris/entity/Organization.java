package com.luccavergara.solaris.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "organizations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Organization {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 13)
    private String cuit;

    @Column(nullable = false)
    private String razonSocial;

    @Column(name = "display_name")
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CondicionIva condicionIva;

    @Column(nullable = false)
    private String timezone;

    @Enumerated(EnumType.STRING)
    @Column(name = "fiscal_provider", nullable = false)
    @Builder.Default
    private FiscalProviderType fiscalProvider = FiscalProviderType.MOCK;

    @Column(name = "fiscal_api_key", columnDefinition = "TEXT")
    private String fiscalApiKey;

    @Column(name = "fiscal_punto_venta")
    private Integer fiscalPuntoVenta;

    @Enumerated(EnumType.STRING)
    @Column(name = "country_code", nullable = false, length = 2)
    @Builder.Default
    private CountryCode countryCode = CountryCode.AR;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_jurisdiction", nullable = false, length = 10)
    @Builder.Default
    private BillingJurisdiction billingJurisdiction = BillingJurisdiction.AR;

    @Enumerated(EnumType.STRING)
    @Column(name = "fiscal_jurisdiction", nullable = false, length = 30)
    @Builder.Default
    private FiscalJurisdiction fiscalJurisdiction = FiscalJurisdiction.AR_AFIP;

    @Column(name = "default_currency", nullable = false, length = 3)
    @Builder.Default
    private String defaultCurrency = "ARS";

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
