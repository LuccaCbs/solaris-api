package com.luccavergara.solaris.repository;

import com.luccavergara.solaris.entity.Organization;
import com.luccavergara.solaris.entity.Store;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StoreRepository extends JpaRepository<Store, Long> {

    List<Store> findAllByOrganization(Organization organization);

    Optional<Store> findByOrganizationAndName(Organization organization, String name);
}
