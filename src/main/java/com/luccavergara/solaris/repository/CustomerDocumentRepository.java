package com.luccavergara.solaris.repository;

import com.luccavergara.solaris.entity.CustomerDocument;
import com.luccavergara.solaris.entity.DocumentType;
import com.luccavergara.solaris.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerDocumentRepository extends JpaRepository<CustomerDocument, Long> {

    @Query("""
            SELECT COUNT(cd) > 0 FROM CustomerDocument cd
            JOIN cd.customer c
            WHERE c.organization.id = :organizationId
              AND cd.documentType = :documentType
              AND cd.documentNumber = :documentNumber
              AND (:excludeCustomerId IS NULL OR c.id <> :excludeCustomerId)
            """)
    boolean existsByDocumentInOrganization(
            @Param("organizationId") Long organizationId,
            @Param("documentType") DocumentType documentType,
            @Param("documentNumber") String documentNumber,
            @Param("excludeCustomerId") Long excludeCustomerId
    );

    @Query("""
            SELECT COUNT(cd) > 0 FROM CustomerDocument cd
            JOIN cd.customer c
            WHERE c.organization IS NULL
              AND cd.user = :user
              AND cd.documentType = :documentType
              AND cd.documentNumber = :documentNumber
              AND (:excludeCustomerId IS NULL OR c.id <> :excludeCustomerId)
            """)
    boolean existsByDocumentForPersonalUser(
            @Param("user") User user,
            @Param("documentType") DocumentType documentType,
            @Param("documentNumber") String documentNumber,
            @Param("excludeCustomerId") Long excludeCustomerId
    );
}
