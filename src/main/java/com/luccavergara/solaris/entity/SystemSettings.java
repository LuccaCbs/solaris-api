package com.luccavergara.solaris.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalTime;

import java.time.LocalDateTime;

@Entity
@Table(name = "system_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer globalLowStockThreshold;

    @Column
    private String adminAccessPasswordHash;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private String businessTimezone;

    @Column(nullable = false)
    private LocalTime cashRegisterAutoCloseTime;

    @Column(nullable = false)
    private Boolean whatsappEnabled;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

}