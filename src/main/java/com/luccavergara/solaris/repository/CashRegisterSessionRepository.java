package com.luccavergara.solaris.repository;

import com.luccavergara.solaris.entity.CashRegisterSession;
import com.luccavergara.solaris.entity.CashRegisterStatus;
import com.luccavergara.solaris.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CashRegisterSessionRepository extends JpaRepository<CashRegisterSession, Long> {

    Optional<CashRegisterSession> findFirstByStatusAndUserOrderByOpenedAtDesc(
            CashRegisterStatus status,
            User user
    );

    Optional<CashRegisterSession> findFirstByStatusAndOrganizationIdOrderByOpenedAtDesc(
            CashRegisterStatus status,
            Long organizationId
    );

    Optional<CashRegisterSession> findFirstByUserAndOpenedAtBetweenOrderByOpenedAtDesc(
            User user,
            LocalDateTime start,
            LocalDateTime end
    );

    Optional<CashRegisterSession> findFirstByOrganizationIdAndOpenedAtBetweenOrderByOpenedAtDesc(
            Long organizationId,
            LocalDateTime start,
            LocalDateTime end
    );

    Optional<CashRegisterSession> findFirstByStatusAndUserAndOpenedAtBetweenOrderByOpenedAtDesc(
            CashRegisterStatus status,
            User user,
            LocalDateTime start,
            LocalDateTime end
    );

    Optional<CashRegisterSession> findFirstByStatusAndOrganizationIdAndOpenedAtBetweenOrderByOpenedAtDesc(
            CashRegisterStatus status,
            Long organizationId,
            LocalDateTime start,
            LocalDateTime end
    );

    Optional<CashRegisterSession> findByIdAndUser(Long id, User user);

    Optional<CashRegisterSession> findByIdAndOrganizationId(Long id, Long organizationId);

    List<CashRegisterSession> findAllByStatus(CashRegisterStatus status);
}
