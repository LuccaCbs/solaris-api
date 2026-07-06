package com.luccavergara.solaris.repository;

import com.luccavergara.solaris.entity.Customer;
import com.luccavergara.solaris.entity.DocumentType;
import com.luccavergara.solaris.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    List<Customer> findAllByUserOrderByCreatedAtDesc(User user);

    List<Customer> findAllByOrganizationIdOrderByCreatedAtDesc(Long organizationId);

    Optional<Customer> findByIdAndUser(Long id, User user);

    Optional<Customer> findByIdAndOrganizationId(Long id, Long organizationId);

    boolean existsByDocumentTypeAndDocumentNumberAndOrganizationId(
            DocumentType documentType,
            String documentNumber,
            Long organizationId
    );

    boolean existsByDocumentTypeAndDocumentNumberAndOrganizationIdAndIdNot(
            DocumentType documentType,
            String documentNumber,
            Long organizationId,
            Long id
    );

    boolean existsByDocumentTypeAndDocumentNumberAndUserAndOrganizationIsNull(
            DocumentType documentType,
            String documentNumber,
            User user
    );

    boolean existsByDocumentTypeAndDocumentNumberAndUserAndOrganizationIsNullAndIdNot(
            DocumentType documentType,
            String documentNumber,
            User user,
            Long id
    );

    List<Customer> findAllByUserAndActiveTrueOrderByCreatedAtDesc(User user);

    List<Customer> findAllByOrganizationIdAndActiveTrueOrderByCreatedAtDesc(Long organizationId);

    @Query("""
            SELECT c FROM Customer c
            WHERE c.organization.id = :organizationId
              AND c.active = true
              AND (LOWER(c.razonSocial) LIKE LOWER(CONCAT('%', :query, '%'))
                   OR c.documentNumber LIKE CONCAT('%', :query, '%'))
            ORDER BY c.createdAt DESC
            """)
    List<Customer> searchActiveByOrganization(
            @Param("organizationId") Long organizationId,
            @Param("query") String query
    );

    @Query("""
            SELECT c FROM Customer c
            WHERE c.user.id = :userId
              AND c.organization IS NULL
              AND c.active = true
              AND (LOWER(c.razonSocial) LIKE LOWER(CONCAT('%', :query, '%'))
                   OR c.documentNumber LIKE CONCAT('%', :query, '%'))
            ORDER BY c.createdAt DESC
            """)
    List<Customer> searchActiveByUser(
            @Param("userId") Long userId,
            @Param("query") String query
    );
}
