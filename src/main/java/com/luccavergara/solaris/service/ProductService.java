package com.luccavergara.solaris.service;

import com.luccavergara.solaris.dto.ProductRequest;
import com.luccavergara.solaris.dto.ProductResponse;
import com.luccavergara.solaris.dto.ProductUpdateRequest;
import com.luccavergara.solaris.entity.Category;
import com.luccavergara.solaris.entity.Product;
import com.luccavergara.solaris.entity.User;
import com.luccavergara.solaris.exception.DuplicateResourceException;
import com.luccavergara.solaris.exception.ResourceNotFoundException;
import com.luccavergara.solaris.repository.CategoryRepository;
import com.luccavergara.solaris.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final SystemSettingsService systemSettingsService;
    private final AuthenticatedUserService authenticatedUserService;

    public ProductResponse createProduct(ProductRequest request) {
        User currentUser = authenticatedUserService.getCurrentUser();

        if (productRepository.existsBySkuAndUser(request.getSku(), currentUser)) {
            throw new DuplicateResourceException("Product SKU already exists");
        }

        Category category = categoryRepository.findByIdAndUser(request.getCategoryId(), currentUser)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .sku(request.getSku())
                .price(request.getPrice())
                .stockQuantity(request.getStockQuantity())
                .lowStockThreshold(request.getLowStockThreshold())
                .createdAt(LocalDateTime.now())
                .category(category)
                .user(currentUser)
                .build();

        Product savedProduct = productRepository.save(product);

        return mapToResponse(savedProduct);
    }

    public List<ProductResponse> getAllProducts(String search) {
        User currentUser = authenticatedUserService.getCurrentUser();

        List<Product> products;

        if (search == null || search.isBlank()) {
            products = productRepository.findAllByUser(currentUser);
        } else {
            products = productRepository
                    .findByUserAndNameContainingIgnoreCaseOrUserAndSkuContainingIgnoreCaseOrUserAndDescriptionContainingIgnoreCase(
                            currentUser,
                            search,
                            currentUser,
                            search,
                            currentUser,
                            search
                    );
        }

        return products.stream()
                .map(this::mapToResponse)
                .toList();
    }

    public ProductResponse getProductById(Long id) {
        User currentUser = authenticatedUserService.getCurrentUser();

        Product product = productRepository.findByIdAndUser(id, currentUser)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        return mapToResponse(product);
    }

    public ProductResponse updateProduct(Long id, ProductUpdateRequest request) {
        User currentUser = authenticatedUserService.getCurrentUser();

        Product product = productRepository.findByIdAndUser(id, currentUser)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        if (!product.getSku().equals(request.getSku())
                && productRepository.existsBySkuAndUser(request.getSku(), currentUser)) {
            throw new DuplicateResourceException("Product SKU already exists");
        }

        Category category = categoryRepository.findByIdAndUser(request.getCategoryId(), currentUser)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setSku(request.getSku());
        product.setPrice(request.getPrice());
        product.setCategory(category);
        product.setLowStockThreshold(request.getLowStockThreshold());

        Product updatedProduct = productRepository.save(product);

        return mapToResponse(updatedProduct);
    }

    public void deleteProduct(Long id) {
        User currentUser = authenticatedUserService.getCurrentUser();

        Product product = productRepository.findByIdAndUser(id, currentUser)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        productRepository.delete(product);
    }

    private ProductResponse mapToResponse(Product product) {
        Integer globalLowStockThreshold = systemSettingsService
                .getOrCreateSettings()
                .getGlobalLowStockThreshold();

        Integer effectiveLowStockThreshold = product.getLowStockThreshold() != null
                ? product.getLowStockThreshold()
                : globalLowStockThreshold;

        boolean lowStock = product.getStockQuantity() <= effectiveLowStockThreshold;

        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .sku(product.getSku())
                .price(product.getPrice())
                .stockQuantity(product.getStockQuantity())
                .lowStockThreshold(product.getLowStockThreshold())
                .effectiveLowStockThreshold(effectiveLowStockThreshold)
                .lowStock(lowStock)
                .createdAt(product.getCreatedAt())
                .categoryId(product.getCategory() != null ? product.getCategory().getId() : null)
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                .build();
    }
}