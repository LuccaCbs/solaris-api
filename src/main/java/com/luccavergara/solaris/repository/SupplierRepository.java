package com.luccavergara.solaris.repository;

import com.luccavergara.solaris.entity.Supplier;
import com.luccavergara.solaris.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SupplierRepository extends JpaRepository<Supplier, Long> {

    List<Supplier> findAllByUserOrderByCreatedAtDesc(User user);

    List<Supplier> findAllByOrganizationIdOrderByCreatedAtDesc(Long organizationId);

    Optional<Supplier> findByIdAndUser(Long id, User user);

    Optional<Supplier> findByIdAndOrganizationId(Long id, Long organizationId);
}
