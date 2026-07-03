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

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
