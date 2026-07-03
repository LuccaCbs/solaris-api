package com.luccavergara.solaris.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "promo_codes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromoCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String code;

    @Column(name = "code_normalized", nullable = false, length = 64, unique = true)
    private String codeNormalized;

    @Enumerated(EnumType.STRING)
    @Column(name = "promo_type", nullable = false, length = 50)
    private PromoCodeType promoType;

    @Enumerated(EnumType.STRING)
    @Column(name = "grant_plan_code", length = 50)
    private SubscriptionPlanCode grantPlanCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "grant_module_code", length = 50)
    private ModuleCode grantModuleCode;

    @Column(name = "duration_days")
    private Integer durationDays;

    @Column(name = "max_redemptions")
    private Integer maxRedemptions;

    @Column(name = "redemption_count", nullable = false)
    @Builder.Default
    private Integer redemptionCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private PromoCodeStatus status = PromoCodeStatus.ACTIVE;

    @Column(name = "valid_from", nullable = false)
    private LocalDateTime validFrom;

    @Column(name = "valid_until")
    private LocalDateTime validUntil;

    @Column(name = "internal_note", columnDefinition = "TEXT")
    private String internalNote;

    @Column(name = "created_by_user_id")
    private Long createdByUserId;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "revoked_by_user_id")
    private Long revokedByUserId;

    @Column(name = "revoke_reason", columnDefinition = "TEXT")
    private String revokeReason;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();

        if (createdAt == null) {
            createdAt = now;
        }

        if (updatedAt == null) {
            updatedAt = now;
        }

        if (validFrom == null) {
            validFrom = now;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public boolean isRedeemableAt(LocalDateTime now) {
        if (status != PromoCodeStatus.ACTIVE) {
            return false;
        }

        if (validFrom != null && now.isBefore(validFrom)) {
            return false;
        }

        if (validUntil != null && !now.isBefore(validUntil)) {
            return false;
        }

        if (maxRedemptions != null && redemptionCount >= maxRedemptions) {
            return false;
        }

        return true;
    }
}
