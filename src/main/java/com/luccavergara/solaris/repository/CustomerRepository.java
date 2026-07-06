package com.luccavergara.solaris.repository;

import com.luccavergara.solaris.entity.Customer;
import com.luccavergara.solaris.entity.DocumentType;
import com.luccavergara.solaris.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    @EntityGraph(attributePaths = "documents")
    List<Customer> findAllByUserOrderByCreatedAtDesc(User user);

    @EntityGraph(attributePaths = "documents")
    List<Customer> findAllByOrganizationIdOrderByCreatedAtDesc(Long organizationId);

    @EntityGraph(attributePaths = "documents")
    Optional<Customer> findByIdAndUser(Long id, User user);

    @EntityGraph(attributePaths = "documents")
    Optional<Customer> findByIdAndOrganizationId(Long id, Long organizationId);

    @EntityGraph(attributePaths = "documents")
    List<Customer> findAllByUserAndActiveTrueOrderByCreatedAtDesc(User user);

    @EntityGraph(attributePaths = "documents")
    List<Customer> findAllByOrganizationIdAndActiveTrueOrderByCreatedAtDesc(Long organizationId);

    @EntityGraph(attributePaths = "documents")
    @Query("""
            SELECT DISTINCT c FROM Customer c
            LEFT JOIN c.documents d
            WHERE c.organization.id = :organizationId
              AND c.active = true
              AND (LOWER(c.razonSocial) LIKE LOWER(CONCAT('%', :query, '%'))
                   OR c.documentNumber LIKE CONCAT('%', :query, '%')
                   OR d.documentNumber LIKE CONCAT('%', :query, '%'))
            ORDER BY c.createdAt DESC
            """)
    List<Customer> searchActiveByOrganization(
            @Param("organizationId") Long organizationId,
            @Param("query") String query
    );

    @EntityGraph(attributePaths = "documents")
    @Query("""
            SELECT DISTINCT c FROM Customer c
            LEFT JOIN c.documents d
            WHERE c.user.id = :userId
              AND c.organization IS NULL
              AND c.active = true
              AND (LOWER(c.razonSocial) LIKE LOWER(CONCAT('%', :query, '%'))
                   OR c.documentNumber LIKE CONCAT('%', :query, '%')
                   OR d.documentNumber LIKE CONCAT('%', :query, '%'))
            ORDER BY c.createdAt DESC
            """)
    List<Customer> searchActiveByUser(
            @Param("userId") Long userId,
            @Param("query") String query
    );
}
