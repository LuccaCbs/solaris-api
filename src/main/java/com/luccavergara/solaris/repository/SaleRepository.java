package com.luccavergara.solaris.repository;

import com.luccavergara.solaris.entity.Sale;
import com.luccavergara.solaris.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SaleRepository extends JpaRepository<Sale, Long> {

    List<Sale> findAllByUserOrderByCreatedAtDesc(User user);

    List<Sale> findAllByOrganizationIdOrderByCreatedAtDesc(Long organizationId);

    Optional<Sale> findByIdAndUser(Long id, User user);

    Optional<Sale> findByIdAndOrganizationId(Long id, Long organizationId);

    List<Sale> findByUserAndCreatedAtBetweenOrderByCreatedAtDesc(
            User user,
            LocalDateTime start,
            LocalDateTime end
    );

    List<Sale> findByOrganizationIdAndCreatedAtBetweenOrderByCreatedAtDesc(
            Long organizationId,
            LocalDateTime start,
            LocalDateTime end
    );

    List<Sale> findAllByCashRegisterSessionIdAndUser(
            Long cashRegisterSessionId,
            User user
    );

    List<Sale> findAllByCashRegisterSessionIdAndOrganizationId(
            Long cashRegisterSessionId,
            Long organizationId
    );
}
