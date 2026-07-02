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

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
