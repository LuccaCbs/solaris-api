package com.luccavergara.solaris.repository;

import com.luccavergara.solaris.entity.Category;
import com.luccavergara.solaris.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findAllByUser(User user);

    List<Category> findAllByOrganizationId(Long organizationId);

    Optional<Category> findByIdAndUser(Long id, User user);

    Optional<Category> findByIdAndOrganizationId(Long id, Long organizationId);

    Optional<Category> findByNameIgnoreCaseAndUser(String name, User user);

    Optional<Category> findByNameIgnoreCaseAndOrganizationId(String name, Long organizationId);

    boolean existsByNameIgnoreCaseAndUser(String name, User user);

    boolean existsByNameIgnoreCaseAndOrganizationId(String name, Long organizationId);
}
