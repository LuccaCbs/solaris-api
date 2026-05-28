package com.luccavergara.solaris.repository;

import com.luccavergara.solaris.entity.SupplierOrder;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupplierOrderRepository extends JpaRepository<SupplierOrder, Long> {
}