package com.luccavergara.solaris.dto;

import com.luccavergara.solaris.entity.AuditAction;
import com.luccavergara.solaris.entity.AuditEntityType;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLogResponse {

    private Long id;
    private AuditAction action;
    private AuditEntityType entityType;
    private Long entityId;
    private String description;
    private Long userId;
    private String userEmail;
    private LocalDateTime createdAt;
    private String userName;
    private String entityName;
}