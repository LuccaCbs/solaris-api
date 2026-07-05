package com.luccavergara.solaris.repository;

import com.luccavergara.solaris.entity.Product;
import com.luccavergara.solaris.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findByBarcodeAndUser(String barcode, User user);

    Optional<Product> findByBarcodeAndOrganizationId(String barcode, Long organizationId);

    Optional<Product> findByIdAndUser(Long id, User user);

    Optional<Product> findByIdAndOrganizationId(Long id, Long organizationId);

    boolean existsByBarcodeAndUser(String barcode, User user);

    boolean existsByBarcodeAndOrganizationId(String barcode, Long organizationId);

    List<Product> findAllByUser(User user);

    List<Product> findAllByOrganizationId(Long organizationId);

    List<Product> findByUserAndNameContainingIgnoreCaseOrUserAndBarcodeContainingIgnoreCaseOrUserAndDescriptionContainingIgnoreCase(
            User user1,
            String name,
            User user2,
            String barcode,
            User user3,
            String description
    );

    List<Product> findByOrganizationIdAndNameContainingIgnoreCaseOrOrganizationIdAndBarcodeContainingIgnoreCaseOrOrganizationIdAndDescriptionContainingIgnoreCase(
            Long organizationId1,
            String name,
            Long organizationId2,
            String barcode,
            Long organizationId3,
            String description
    );

    List<Product> findByUserAndBarcodeStartingWith(User user, String barcodePrefix);

    List<Product> findByOrganizationIdAndBarcodeStartingWith(Long organizationId, String barcodePrefix);

    List<Product> findByBarcodeStartingWith(String barcodePrefix);

    boolean existsByBarcode(String barcode);

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

    List<Product> findByUserAndActiveTrueAndNameContainingIgnoreCaseOrUserAndActiveTrueAndBarcodeContainingIgnoreCaseOrUserAndActiveTrueAndDescriptionContainingIgnoreCase(
            User user1,
            String name,
            User user2,
            String barcode,
            User user3,
            String description
    );

    List<Product> findByOrganizationIdAndActiveTrueAndNameContainingIgnoreCaseOrOrganizationIdAndActiveTrueAndBarcodeContainingIgnoreCaseOrOrganizationIdAndActiveTrueAndDescriptionContainingIgnoreCase(
            Long organizationId1,
            String name,
            Long organizationId2,
            String barcode,
            Long organizationId3,
            String description
    );
}
