package com.luccavergara.solaris.service;

import com.luccavergara.solaris.entity.AuditAction;
import com.luccavergara.solaris.entity.AuditEntityType;
import com.luccavergara.solaris.entity.AuditLog;
import com.luccavergara.solaris.entity.User;
import com.luccavergara.solaris.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.luccavergara.solaris.dto.AuditLogResponse;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final AuthenticatedUserService authenticatedUserService;
    private final TenantQueryService tenantQueryService;

    public void log(
            AuditAction action,
            AuditEntityType entityType,
            Long entityId,
            String entityName,
            String description
    ) {
        User currentUser = null;

        try {
            currentUser = authenticatedUserService.getCurrentUser();
        } catch (Exception ignored) {
            // Allows audit logs from unauthenticated flows if needed.
        }

        AuditLog auditLog = AuditLog.builder()
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .entityName(entityName)
                .description(description)
                .userId(currentUser != null ? currentUser.getId() : null)
                .userEmail(currentUser != null ? currentUser.getEmail() : null)
                .userName(currentUser != null ? buildUserName(currentUser) : "System")
                .build();

        auditLogRepository.save(auditLog);
    }

    private String buildUserName(User user) {
        String firstname = user.getFirstname() != null ? user.getFirstname() : "";
        String lastname = user.getLastname() != null ? user.getLastname() : "";

        String fullName = (firstname + " " + lastname).trim();

        return fullName.isBlank() ? user.getEmail() : fullName;
    }


    public List<AuditLogResponse> getAuditLogs() {
        return tenantQueryService.findAuditLogs()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private AuditLogResponse mapToResponse(AuditLog auditLog) {
        return AuditLogResponse.builder()
                .id(auditLog.getId())
                .action(auditLog.getAction())
                .entityType(auditLog.getEntityType())
                .entityId(auditLog.getEntityId())
                .entityName(auditLog.getEntityName())
                .description(auditLog.getDescription())
                .userId(auditLog.getUserId())
                .userEmail(auditLog.getUserEmail())
                .userName(auditLog.getUserName())
                .createdAt(auditLog.getCreatedAt())
                .build();
    }
}