package com.luccavergara.solaris.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "cash_register_reopen_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CashRegisterReopenLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cash_register_session_id", nullable = false)
    private CashRegisterSession cashRegisterSession;

    @Column(nullable = false)
    private String reopenedBy;

    @Column(nullable = false)
    private LocalDateTime reopenedAt;
}