package com.luccavergara.solaris.repository;

import com.luccavergara.solaris.entity.SupplierOrder;
import com.luccavergara.solaris.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SupplierOrderRepository extends JpaRepository<SupplierOrder, Long> {

    List<SupplierOrder> findAllByUser(User user);

    List<SupplierOrder> findAllByOrganizationId(Long organizationId);

    Optional<SupplierOrder> findByIdAndUser(Long id, User user);

    Optional<SupplierOrder> findByIdAndOrganizationId(Long id, Long organizationId);
}
