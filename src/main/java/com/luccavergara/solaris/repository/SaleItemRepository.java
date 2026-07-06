package com.luccavergara.solaris.repository;

import com.luccavergara.solaris.entity.SaleItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface SaleItemRepository extends JpaRepository<SaleItem, Long> {

    @Query("""
            SELECT COALESCE(SUM(si.quantity), 0)
            FROM SaleItem si
            JOIN si.sale s
            WHERE si.product.id = :productId
              AND s.organization.id = :organizationId
              AND s.createdAt >= :start
              AND s.createdAt < :end
            """)
    Integer sumQuantityByProductAndOrganizationAndDateRange(
            @Param("productId") Long productId,
            @Param("organizationId") Long organizationId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query("""
            SELECT COALESCE(SUM(si.quantity), 0)
            FROM SaleItem si
            JOIN si.sale s
            WHERE si.product.id = :productId
              AND s.user.id = :userId
              AND s.organization IS NULL
              AND s.createdAt >= :start
              AND s.createdAt < :end
            """)
    Integer sumQuantityByProductAndUserAndDateRange(
            @Param("productId") Long productId,
            @Param("userId") Long userId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query("""
            SELECT COALESCE(SUM(si.subtotal), 0)
            FROM SaleItem si
            JOIN si.sale s
            WHERE si.product.id = :productId
              AND s.organization.id = :organizationId
              AND s.createdAt >= :start
              AND s.createdAt < :end
            """)
    BigDecimal sumRevenueByProductAndOrganizationAndDateRange(
            @Param("productId") Long productId,
            @Param("organizationId") Long organizationId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query("""
            SELECT COALESCE(SUM(si.subtotal), 0)
            FROM SaleItem si
            JOIN si.sale s
            WHERE si.product.id = :productId
              AND s.user.id = :userId
              AND s.organization IS NULL
              AND s.createdAt >= :start
              AND s.createdAt < :end
            """)
    BigDecimal sumRevenueByProductAndUserAndDateRange(
            @Param("productId") Long productId,
            @Param("userId") Long userId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );
}
