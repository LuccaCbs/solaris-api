package com.luccavergara.solaris.dto;

import com.luccavergara.solaris.entity.OrganizationMemberRole;
import com.luccavergara.solaris.entity.OrganizationMemberStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrganizationMemberResponse {

    private Long id;
    private String email;
    private String firstname;
    private String lastname;
    private OrganizationMemberRole role;
    private OrganizationMemberStatus status;
    private Long storeId;
    private String storeName;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private boolean pendingInvite;
}
