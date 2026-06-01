package com.luccavergara.solaris.repository;

import com.luccavergara.solaris.entity.CashRegisterSession;
import com.luccavergara.solaris.entity.CashRegisterStatus;
import com.luccavergara.solaris.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;

public interface CashRegisterSessionRepository extends JpaRepository<CashRegisterSession, Long> {

    Optional<CashRegisterSession> findFirstByStatusAndUserOrderByOpenedAtDesc(
            CashRegisterStatus status,
            User user
    );

    Optional<CashRegisterSession> findFirstByUserAndOpenedAtBetweenOrderByOpenedAtDesc(
            User user,
            LocalDateTime start,
            LocalDateTime end
    );

    Optional<CashRegisterSession> findFirstByStatusAndUserAndOpenedAtBetweenOrderByOpenedAtDesc(
            CashRegisterStatus status,
            User user,
            LocalDateTime start,
            LocalDateTime end
    );

    Optional<CashRegisterSession> findByIdAndUser(Long id, User user);

    List<CashRegisterSession> findAllByStatus(CashRegisterStatus status);
}