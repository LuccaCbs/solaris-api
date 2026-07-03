package com.luccavergara.solaris.repository;

import com.luccavergara.solaris.entity.Organization;
import com.luccavergara.solaris.entity.OrganizationSubscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrganizationSubscriptionRepository extends JpaRepository<OrganizationSubscription, Long> {

    Optional<OrganizationSubscription> findByOrganizationId(Long organizationId);

    Optional<OrganizationSubscription> findByOrganization(Organization organization);
}
