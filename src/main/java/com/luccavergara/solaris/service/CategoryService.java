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

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final AuthenticatedUserService authenticatedUserService;

    public CategoryResponse createCategory(CategoryRequest request) {
        User currentUser = authenticatedUserService.getCurrentUser();

        if (categoryRepository.existsByNameIgnoreCaseAndUser(request.getName(), currentUser)) {
            throw new DuplicateResourceException("Category name already exists");
        }

        Category category = Category.builder()
                .name(request.getName())
                .description(request.getDescription())
                .createdAt(LocalDateTime.now())
                .user(currentUser)
                .build();

        return mapToResponse(categoryRepository.save(category));
    }

    public List<CategoryResponse> getAllCategories() {
        User currentUser = authenticatedUserService.getCurrentUser();

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

        if (!category.getName().equalsIgnoreCase(request.getName())
                && categoryRepository.existsByNameIgnoreCaseAndUser(request.getName(), currentUser)) {
            throw new DuplicateResourceException("Category name already exists");
        }

        category.setName(request.getName());
        category.setDescription(request.getDescription());

        return mapToResponse(categoryRepository.save(category));
    }

    public void deleteCategory(Long id) {
        User currentUser = authenticatedUserService.getCurrentUser();

        Category category = categoryRepository.findByIdAndUser(id, currentUser)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        categoryRepository.delete(category);
    }

    private CategoryResponse mapToResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .createdAt(category.getCreatedAt())
                .build();
    }
}