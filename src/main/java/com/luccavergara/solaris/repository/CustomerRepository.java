package com.luccavergara.solaris.repository;

import com.luccavergara.solaris.entity.Customer;
import com.luccavergara.solaris.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    List<Customer> findAllByUserOrderByCreatedAtDesc(User user);

    List<Customer> findAllByOrganizationIdOrderByCreatedAtDesc(Long organizationId);

    Optional<Customer> findByIdAndUser(Long id, User user);

    Optional<Customer> findByIdAndOrganizationId(Long id, Long organizationId);
}
