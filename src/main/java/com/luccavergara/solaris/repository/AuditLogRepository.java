package com.luccavergara.solaris.repository;

import com.luccavergara.solaris.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findAllByOrderByCreatedAtDesc();

    List<AuditLog> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    List<AuditLog> findAllByUserIdInOrderByCreatedAtDesc(Collection<Long> userIds);
}