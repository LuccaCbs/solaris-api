package com.luccavergara.solaris.repository;

import com.luccavergara.solaris.entity.Customer;
import com.luccavergara.solaris.entity.DocumentType;
import com.luccavergara.solaris.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
