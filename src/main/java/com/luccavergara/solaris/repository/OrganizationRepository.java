package com.luccavergara.solaris.repository;

import com.luccavergara.solaris.entity.Organization;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationRepository extends JpaRepository<Organization, Long> {
}
