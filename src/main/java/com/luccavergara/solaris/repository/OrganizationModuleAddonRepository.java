package com.luccavergara.solaris.repository;

import com.luccavergara.solaris.entity.ModuleCode;
import com.luccavergara.solaris.entity.ModuleAddonStatus;
import com.luccavergara.solaris.entity.OrganizationModuleAddon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface OrganizationModuleAddonRepository extends JpaRepository<OrganizationModuleAddon, Long> {

    @Query("""
            SELECT addon.moduleCode
            FROM OrganizationModuleAddon addon
            WHERE addon.organization.id = :organizationId
              AND addon.status = :status
              AND addon.validFrom <= :now
              AND (addon.validUntil IS NULL OR addon.validUntil > :now)
            """)
    List<ModuleCode> findActiveModuleCodesByOrganizationId(
            @Param("organizationId") Long organizationId,
            @Param("status") ModuleAddonStatus status,
            @Param("now") LocalDateTime now
    );

    @Query("""
            SELECT addon.moduleCode
            FROM OrganizationModuleAddon addon
            WHERE addon.organization.id = :organizationId
              AND addon.sourceType = :sourceType
              AND addon.status = :status
              AND addon.validFrom <= :now
              AND (addon.validUntil IS NULL OR addon.validUntil > :now)
            """)
    List<ModuleCode> findActiveModuleCodesByOrganizationIdAndSourceType(
            @Param("organizationId") Long organizationId,
            @Param("sourceType") com.luccavergara.solaris.entity.ModuleAddonSourceType sourceType,
            @Param("status") ModuleAddonStatus status,
            @Param("now") LocalDateTime now
    );

    java.util.Optional<OrganizationModuleAddon> findByOrganizationIdAndModuleCodeAndSourceType(
            Long organizationId,
            ModuleCode moduleCode,
            com.luccavergara.solaris.entity.ModuleAddonSourceType sourceType
    );
}
