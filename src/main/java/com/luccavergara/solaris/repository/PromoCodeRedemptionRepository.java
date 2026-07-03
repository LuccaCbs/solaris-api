package com.luccavergara.solaris.repository;

import com.luccavergara.solaris.entity.PromoCodeRedemption;
import com.luccavergara.solaris.entity.PromoRedemptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface PromoCodeRedemptionRepository extends JpaRepository<PromoCodeRedemption, Long> {

    @Query("""
            SELECT redemption
            FROM PromoCodeRedemption redemption
            WHERE redemption.organization.id = :organizationId
              AND redemption.status = :status
              AND redemption.accessValidFrom <= :now
              AND (redemption.accessValidUntil IS NULL OR redemption.accessValidUntil > :now)
            """)
    List<PromoCodeRedemption> findActiveRedemptionsByOrganizationId(
            @Param("organizationId") Long organizationId,
            @Param("status") PromoRedemptionStatus status,
            @Param("now") LocalDateTime now
    );
}
