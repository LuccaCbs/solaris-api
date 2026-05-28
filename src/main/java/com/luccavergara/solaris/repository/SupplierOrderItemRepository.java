package com.luccavergara.solaris.repository;

import com.luccavergara.solaris.entity.SupplierOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupplierOrderItemRepository extends JpaRepository<SupplierOrderItem, Long> {
}