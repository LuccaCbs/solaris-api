package com.luccavergara.solaris.repository;

import com.luccavergara.solaris.entity.OrganizationInvite;
import com.luccavergara.solaris.entity.OrganizationInviteStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrganizationInviteRepository extends JpaRepository<OrganizationInvite, Long> {

    Optional<OrganizationInvite> findByToken(String token);

    List<OrganizationInvite> findAllByOrganizationIdAndStatus(
            Long organizationId,
            OrganizationInviteStatus status
    );

    Optional<OrganizationInvite> findByOrganizationIdAndEmailIgnoreCaseAndStatus(
            Long organizationId,
            String email,
            OrganizationInviteStatus status
    );

    Optional<OrganizationInvite> findByIdAndOrganizationId(Long id, Long organizationId);
}
