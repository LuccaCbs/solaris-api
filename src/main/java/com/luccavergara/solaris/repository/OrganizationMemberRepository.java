package com.luccavergara.solaris.repository;

import com.luccavergara.solaris.entity.OrganizationMember;
import com.luccavergara.solaris.entity.OrganizationMemberStatus;
import com.luccavergara.solaris.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrganizationMemberRepository extends JpaRepository<OrganizationMember, Long> {

    boolean existsByUser(User user);

    Optional<OrganizationMember> findFirstByUserAndStatus(
            User user,
            OrganizationMemberStatus status
    );

    List<OrganizationMember> findAllByUser(User user);

    List<OrganizationMember> findAllByUserAndStatus(User user, OrganizationMemberStatus status);

    Optional<OrganizationMember> findByUserAndOrganizationIdAndStatus(
            User user,
            Long organizationId,
            OrganizationMemberStatus status
    );

    Optional<OrganizationMember> findFirstByUserAndRoleAndStatus(
            User user,
            com.luccavergara.solaris.entity.OrganizationMemberRole role,
            OrganizationMemberStatus status
    );

    List<OrganizationMember> findAllByOrganizationId(Long organizationId);

    Optional<OrganizationMember> findByUserAndOrganizationId(User user, Long organizationId);

    @Query("""
            SELECT CASE WHEN COUNT(om) > 0 THEN true ELSE false END
            FROM OrganizationMember om
            WHERE om.organization.id = :organizationId
              AND LOWER(om.user.email) = LOWER(:email)
              AND om.status = :status
            """)
    boolean existsByOrganizationIdAndUserEmailIgnoreCaseAndStatus(
            @Param("organizationId") Long organizationId,
            @Param("email") String email,
            @Param("status") OrganizationMemberStatus status
    );

    @Query("SELECT om.user.id FROM OrganizationMember om WHERE om.organization.id = :organizationId AND om.status = :status")
    List<Long> findUserIdsByOrganizationIdAndStatus(
            @Param("organizationId") Long organizationId,
            @Param("status") OrganizationMemberStatus status
    );
}
