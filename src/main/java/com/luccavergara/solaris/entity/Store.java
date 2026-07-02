package com.luccavergara.solaris.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "stores")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Store {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(nullable = false)
    private String name;

    private String address;

    @Column(name = "afip_punto_venta")
    private Integer afipPuntoVenta;

    @Column(nullable = false)
    private Boolean active;
}
