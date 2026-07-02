package com.luccavergara.solaris.repository;

import com.luccavergara.solaris.entity.OrganizationMember;
import com.luccavergara.solaris.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrganizationMemberRepository extends JpaRepository<OrganizationMember, Long> {

    boolean existsByUser(User user);

    Optional<OrganizationMember> findFirstByUserAndStatus(
            User user,
            com.luccavergara.solaris.entity.OrganizationMemberStatus status
    );

    List<OrganizationMember> findAllByUser(User user);
}
