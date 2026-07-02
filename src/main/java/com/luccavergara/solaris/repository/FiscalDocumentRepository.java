package com.luccavergara.solaris.repository;

import com.luccavergara.solaris.entity.FiscalDocument;
import com.luccavergara.solaris.entity.TipoComprobante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FiscalDocumentRepository extends JpaRepository<FiscalDocument, Long> {

    List<FiscalDocument> findAllByOrganizationIdOrderByCreatedAtDesc(Long organizationId);

    Optional<FiscalDocument> findByIdAndOrganizationId(Long id, Long organizationId);

    Optional<FiscalDocument> findBySaleId(Long saleId);

    Optional<FiscalDocument> findBySaleIdAndOrganizationId(Long saleId, Long organizationId);

    @Query("""
            SELECT COALESCE(MAX(fd.numeroComprobante), 0)
            FROM FiscalDocument fd
            WHERE fd.organization.id = :organizationId
              AND fd.puntoVenta = :puntoVenta
              AND fd.tipoComprobante = :tipoComprobante
            """)
    Long findMaxNumeroComprobante(
            @Param("organizationId") Long organizationId,
            @Param("puntoVenta") Integer puntoVenta,
            @Param("tipoComprobante") TipoComprobante tipoComprobante
    );
}
