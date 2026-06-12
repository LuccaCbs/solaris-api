package com.luccavergara.solaris.service;

import com.luccavergara.solaris.dto.CategoryRequest;
import com.luccavergara.solaris.dto.CategoryResponse;
import com.luccavergara.solaris.entity.Category;
import com.luccavergara.solaris.entity.User;
import com.luccavergara.solaris.exception.DuplicateResourceException;
import com.luccavergara.solaris.exception.ResourceNotFoundException;
import com.luccavergara.solaris.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.luccavergara.solaris.entity.AuditAction;
import com.luccavergara.solaris.entity.AuditEntityType;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private static final String DEFAULT_CATEGORY_NAME = "General";

    private final CategoryRepository categoryRepository;
    private final AuthenticatedUserService authenticatedUserService;
    private final AuditLogService auditLogService;

    public CategoryResponse createCategory(CategoryRequest request) {
        User currentUser = authenticatedUserService.getCurrentUser();

        if (categoryRepository.existsByNameIgnoreCaseAndUser(request.getName(), currentUser)) {
            throw new DuplicateResourceException("Category name already exists");
        }

        Category category = Category.builder()
                .name(request.getName())
                .description(request.getDescription())
                .createdAt(LocalDateTime.now())
                .systemCategory(false)
                .user(currentUser)
                .build();

        Category savedCategory = categoryRepository.save(category);

        auditLogService.log(
                AuditAction.CREATE,
                AuditEntityType.CATEGORY,
                savedCategory.getId(),
                savedCategory.getName(),
                "Category created"
        );

        return mapToResponse(savedCategory);
    }

    public List<CategoryResponse> getAllCategories() {
        User currentUser = authenticatedUserService.getCurrentUser();

        ensureDefaultCategoryExists(currentUser);

        return categoryRepository.findAllByUser(currentUser)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public CategoryResponse getCategoryById(Long id) {
        User currentUser = authenticatedUserService.getCurrentUser();

        Category category = categoryRepository.findByIdAndUser(id, currentUser)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        return mapToResponse(category);
    }

    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        User currentUser = authenticatedUserService.getCurrentUser();

        Category category = categoryRepository.findByIdAndUser(id, currentUser)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        if (Boolean.TRUE.equals(category.getSystemCategory())) {
            throw new IllegalStateException("System category cannot be modified");
        }

        if (!category.getName().equalsIgnoreCase(request.getName())
                && categoryRepository.existsByNameIgnoreCaseAndUser(request.getName(), currentUser)) {
            throw new DuplicateResourceException("Category name already exists");
        }

        category.setName(request.getName());
        category.setDescription(request.getDescription());

        Category updatedCategory = categoryRepository.save(category);

        auditLogService.log(
                AuditAction.UPDATE,
                AuditEntityType.CATEGORY,
                updatedCategory.getId(),
                updatedCategory.getName(),
                "Category updated"
        );

        return mapToResponse(updatedCategory);
    }

    public void deleteCategory(Long id) {
        User currentUser = authenticatedUserService.getCurrentUser();

        Category category = categoryRepository.findByIdAndUser(id, currentUser)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        if (Boolean.TRUE.equals(category.getSystemCategory())) {
            throw new IllegalStateException("System category cannot be deleted");
        }

        auditLogService.log(
                AuditAction.DELETE,
                AuditEntityType.CATEGORY,
                category.getId(),
                category.getName(),
                "Category deleted"
        );

        categoryRepository.delete(category);
    }

    public Category getOrCreateDefaultCategory(User user) {
        return categoryRepository.findByNameIgnoreCaseAndUser(DEFAULT_CATEGORY_NAME, user)
                .orElseGet(() -> categoryRepository.save(
                        Category.builder()
                                .name(DEFAULT_CATEGORY_NAME)
                                .description("Default category")
                                .createdAt(LocalDateTime.now())
                                .systemCategory(true)
                                .user(user)
                                .build()
                ));
    }

    private void ensureDefaultCategoryExists(User user) {
        getOrCreateDefaultCategory(user);
    }

    private CategoryResponse mapToResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .systemCategory(category.getSystemCategory())
                .createdAt(category.getCreatedAt())
                .build();
    }
}