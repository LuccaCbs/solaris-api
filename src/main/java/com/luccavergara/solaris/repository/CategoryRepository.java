package com.luccavergara.solaris.repository;

import com.luccavergara.solaris.entity.Category;
import com.luccavergara.solaris.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findAllByUser(User user);

    Optional<Category> findByIdAndUser(Long id, User user);

    boolean existsByNameIgnoreCaseAndUser(String name, User user);
}