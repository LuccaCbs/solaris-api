package com.luccavergara.solaris.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "cash_register_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CashRegisterSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime openedAt;

    private LocalDateTime closedAt;

    @Column(nullable = false)
    private String openedBy;

    private String closedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CashRegisterStatus status;

    @Column(nullable = false)
    private Integer reopenCount;

    private BigDecimal closingAmount;

    @Column(nullable = false)
    private Integer cashCount;

    @Column(nullable = false)
    private BigDecimal cashAmount;

    @Column(nullable = false)
    private Integer creditCardCount;

    @Column(nullable = false)
    private BigDecimal creditCardAmount;

    @Column(nullable = false)
    private Integer debitCardCount;

    @Column(nullable = false)
    private BigDecimal debitCardAmount;

    @Column(nullable = false)
    private Integer transferCount;

    @Column(nullable = false)
    private BigDecimal transferAmount;

    @Column(nullable = false)
    private Integer otherCount;

    @Column(nullable = false)
    private BigDecimal otherAmount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    private User createdBy;

}