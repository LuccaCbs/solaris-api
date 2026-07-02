package com.luccavergara.solaris.repository;

import com.luccavergara.solaris.entity.Product;
import com.luccavergara.solaris.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findBySkuAndUser(String sku, User user);

    Optional<Product> findBySkuAndOrganizationId(String sku, Long organizationId);

    Optional<Product> findByIdAndUser(Long id, User user);

    Optional<Product> findByIdAndOrganizationId(Long id, Long organizationId);

    boolean existsBySkuAndUser(String sku, User user);

    boolean existsBySkuAndOrganizationId(String sku, Long organizationId);

    List<Product> findAllByUser(User user);

    List<Product> findAllByOrganizationId(Long organizationId);

    List<Product> findByUserAndNameContainingIgnoreCaseOrUserAndSkuContainingIgnoreCaseOrUserAndDescriptionContainingIgnoreCase(
            User user1,
            String name,
            User user2,
            String sku,
            User user3,
            String description
    );

    List<Product> findByOrganizationIdAndNameContainingIgnoreCaseOrOrganizationIdAndSkuContainingIgnoreCaseOrOrganizationIdAndDescriptionContainingIgnoreCase(
            Long organizationId1,
            String name,
            Long organizationId2,
            String sku,
            Long organizationId3,
            String description
    );

    List<Product> findByUserAndSkuStartingWith(User user, String skuPrefix);

    List<Product> findByOrganizationIdAndSkuStartingWith(Long organizationId, String skuPrefix);

    boolean existsByNameIgnoreCaseAndUser(String name, User user);

    boolean existsByNameIgnoreCaseAndOrganizationId(String name, Long organizationId);

    boolean existsByNameIgnoreCaseAndUserAndIdNot(
            String name,
            User user,
            Long id
    );

    boolean existsByNameIgnoreCaseAndOrganizationIdAndIdNot(
            String name,
            Long organizationId,
            Long id
    );

    Optional<Product> findByNameIgnoreCaseAndUser(String name, User user);

    Optional<Product> findByNameIgnoreCaseAndOrganizationId(String name, Long organizationId);

    List<Product> findAllByUserAndActiveTrue(User user);

    List<Product> findAllByOrganizationIdAndActiveTrue(Long organizationId);

    List<Product> findByUserAndActiveTrueAndNameContainingIgnoreCaseOrUserAndActiveTrueAndSkuContainingIgnoreCaseOrUserAndActiveTrueAndDescriptionContainingIgnoreCase(
            User user1,
            String name,
            User user2,
            String sku,
            User user3,
            String description
    );

    List<Product> findByOrganizationIdAndActiveTrueAndNameContainingIgnoreCaseOrOrganizationIdAndActiveTrueAndSkuContainingIgnoreCaseOrOrganizationIdAndActiveTrueAndDescriptionContainingIgnoreCase(
            Long organizationId1,
            String name,
            Long organizationId2,
            String sku,
            Long organizationId3,
            String description
    );
}
