package com.luccavergara.solaris.repository;

import com.luccavergara.solaris.entity.Product;
import com.luccavergara.solaris.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findBySkuAndUser(String sku, User user);

    Optional<Product> findByIdAndUser(Long id, User user);

    boolean existsBySkuAndUser(String sku, User user);

    List<Product> findAllByUser(User user);

    List<Product> findByUserAndNameContainingIgnoreCaseOrUserAndSkuContainingIgnoreCaseOrUserAndDescriptionContainingIgnoreCase(
            User user1,
            String name,
            User user2,
            String sku,
            User user3,
            String description
    );
    List<Product> findByUserAndSkuStartingWith(User user, String skuPrefix);

    boolean existsByNameIgnoreCaseAndUser(String name, User user);
    boolean existsByNameIgnoreCaseAndUserAndIdNot(
            String name,
            User user,
            Long id
    );

    Optional<Product> findByNameIgnoreCaseAndUser(String name, User user);
}