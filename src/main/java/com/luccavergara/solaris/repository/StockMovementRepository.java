package com.luccavergara.solaris.repository;

import com.luccavergara.solaris.entity.StockMovement;
import com.luccavergara.solaris.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    List<StockMovement> findAllByUserOrderByCreatedAtDesc(User user);

    List<StockMovement> findByProductIdAndUserOrderByCreatedAtDesc(
            Long productId,
            User user
    );
}