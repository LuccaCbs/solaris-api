package com.luccavergara.solaris.dto;

import com.luccavergara.solaris.entity.OrganizationMemberRole;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrganizationInviteResponse {

    private Long id;
    private String email;
    private OrganizationMemberRole role;
    private Long storeId;
    private String storeName;
    private String status;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
}
