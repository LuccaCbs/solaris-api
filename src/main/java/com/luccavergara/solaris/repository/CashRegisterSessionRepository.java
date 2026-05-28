package com.luccavergara.solaris.repository;

import com.luccavergara.solaris.entity.CashRegisterSession;
import com.luccavergara.solaris.entity.CashRegisterStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface CashRegisterSessionRepository extends JpaRepository<CashRegisterSession, Long> {

    Optional<CashRegisterSession> findFirstByStatusOrderByOpenedAtDesc(
            CashRegisterStatus status
    );

    Optional<CashRegisterSession> findFirstByOpenedAtBetweenOrderByOpenedAtDesc(
            LocalDateTime start,
            LocalDateTime end
    );

    Optional<CashRegisterSession> findFirstByStatusAndOpenedAtBetweenOrderByOpenedAtDesc(
            CashRegisterStatus status,
            LocalDateTime start,
            LocalDateTime end
    );
}