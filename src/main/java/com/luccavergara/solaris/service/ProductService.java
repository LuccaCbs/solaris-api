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
import com.luccavergara.solaris.dto.ProductImportMode;

import com.luccavergara.solaris.dto.ProductImportResponse;
import org.apache.poi.ss.usermodel.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final SystemSettingsService systemSettingsService;
    private final AuthenticatedUserService authenticatedUserService;

    public byte[] generateImportTemplate() {
        try (Workbook workbook = WorkbookFactory.create(true);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Products");

            Row header = sheet.createRow(0);

            String[] headers = {
                    "Name",
                    "SKU",
                    "Price",
                    "Stock Quantity",
                    "Category",
                    "Custom Low Stock",
                    "Description"
            };

            for (int index = 0; index < headers.length; index++) {
                Cell cell = header.createCell(index);
                cell.setCellValue(headers[index]);
                sheet.autoSizeColumn(index);
            }

            Row example = sheet.createRow(1);
            example.createCell(0).setCellValue("Example Product");
            example.createCell(1).setCellValue("");
            example.createCell(2).setCellValue(1000);
            example.createCell(3).setCellValue(10);
            example.createCell(4).setCellValue("General");
            example.createCell(5).setCellValue("");
            example.createCell(6).setCellValue("Optional description");

            for (int index = 0; index < headers.length; index++) {
                sheet.autoSizeColumn(index);
            }

            workbook.write(outputStream);

            return outputStream.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("Could not generate import template");
        }
    }

    private boolean isRowEmpty(Row row) {
        for (int cellIndex = 0; cellIndex <= 6; cellIndex++) {
            Cell cell = row.getCell(cellIndex);

            if (cell != null && !getStringCellValue(cell).isBlank()) {
                return false;
            }
        }

        return true;
    }

    private String getStringCellValue(Cell cell) {
        if (cell == null) {
            return "";
        }

        if (cell.getCellType() == CellType.STRING) {
            return cell.getStringCellValue().trim();
        }

        if (cell.getCellType() == CellType.NUMERIC) {
            double numericValue = cell.getNumericCellValue();

            if (numericValue == Math.floor(numericValue)) {
                return String.valueOf((long) numericValue);
            }

            return String.valueOf(numericValue);
        }

        if (cell.getCellType() == CellType.BOOLEAN) {
            return String.valueOf(cell.getBooleanCellValue());
        }

        return "";
    }

    private BigDecimal getBigDecimalCellValue(Cell cell) {
        if (cell == null) {
            throw new IllegalArgumentException("price is required");
        }

        if (cell.getCellType() == CellType.NUMERIC) {
            return BigDecimal.valueOf(cell.getNumericCellValue());
        }

        String value = getStringCellValue(cell);

        if (value.isBlank()) {
            throw new IllegalArgumentException("price is required");
        }

        return new BigDecimal(value);
    }

    private Integer getIntegerCellValue(Cell cell) {
        if (cell == null) {
            throw new IllegalArgumentException("stock quantity is required");
        }

        if (cell.getCellType() == CellType.NUMERIC) {
            return (int) cell.getNumericCellValue();
        }

        String value = getStringCellValue(cell);

        if (value.isBlank()) {
            throw new IllegalArgumentException("stock quantity is required");
        }

        return Integer.parseInt(value);
    }

    private Integer getOptionalIntegerCellValue(Cell cell) {
        if (cell == null || getStringCellValue(cell).isBlank()) {
            return null;
        }

        return getIntegerCellValue(cell);
    }

    private Long resolveCategoryIdFromName(String categoryName) {
        if (categoryName == null || categoryName.isBlank()) {
            return null;
        }

        User currentUser = authenticatedUserService.getCurrentUser();

        Category category = categoryRepository.findByNameIgnoreCaseAndUser(categoryName.trim(), currentUser)
                .orElseGet(() -> categoryRepository.save(
                        Category.builder()
                                .name(categoryName.trim())
                                .description("")
                                .createdAt(LocalDateTime.now())
                                .systemCategory(false)
                                .user(currentUser)
                                .build()
                ));

        return category.getId();
    }

    private String resolveSku(String requestedSku, User currentUser) {
        if (requestedSku != null && !requestedSku.isBlank()) {
            return requestedSku.trim();
        }

        return generateAutomaticSku(currentUser);
    }

    private String generateAutomaticSku(User currentUser) {
        String prefix = "GEN-";

        int maxNumber = productRepository.findByUserAndSkuStartingWith(currentUser, prefix)
                .stream()
                .map(Product::getSku)
                .map(sku -> sku.substring(prefix.length()))
                .filter(value -> value.matches("\\d+"))
                .mapToInt(Integer::parseInt)
                .max()
                .orElse(0);

        return prefix + String.format("%03d", maxNumber + 1);
    }

    private Category resolveCategory(Long categoryId, User currentUser) {
        if (categoryId == null) {
            return getOrCreateDefaultCategory(currentUser);
        }

        return categoryRepository.findByIdAndUser(categoryId, currentUser)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
    }

    private Category getOrCreateDefaultCategory(User currentUser) {
        return categoryRepository.findByNameIgnoreCaseAndUser("General", currentUser)
                .orElseGet(() -> categoryRepository.save(
                        Category.builder()
                                .name("General")
                                .description("Default category")
                                .createdAt(LocalDateTime.now())
                                .systemCategory(true)
                                .user(currentUser)
                                .build()
                ));
    }

    private boolean updateExistingProductIfPresent(ProductRequest request) {
        User currentUser = authenticatedUserService.getCurrentUser();

        Product existingProduct = productRepository
                .findByNameIgnoreCaseAndUser(request.getName(), currentUser)
                .orElse(null);

        if (existingProduct == null) {
            return false;
        }

        String sku = resolveSkuForImportUpdate(request.getSku(), existingProduct, currentUser);

        if (!existingProduct.getSku().equals(sku)
                && productRepository.existsBySkuAndUser(sku, currentUser)) {
            throw new DuplicateResourceException("Product SKU already exists");
        }

        Category category = resolveCategory(request.getCategoryId(), currentUser);

        existingProduct.setDescription(request.getDescription());
        existingProduct.setSku(sku);
        existingProduct.setPrice(request.getPrice());
        existingProduct.setStockQuantity(request.getStockQuantity());
        existingProduct.setCategory(category);
        existingProduct.setLowStockThreshold(request.getLowStockThreshold());

        productRepository.save(existingProduct);

        return true;
    }

    private String resolveSkuForImportUpdate(
            String requestedSku,
            Product existingProduct,
            User currentUser
    ) {
        if (requestedSku != null && !requestedSku.isBlank()) {
            return requestedSku.trim();
        }

        return existingProduct.getSku();
    }

    public ProductResponse createProduct(ProductRequest request) {
        User currentUser = authenticatedUserService.getCurrentUser();

        if (productRepository.existsByNameIgnoreCaseAndUser(request.getName(), currentUser)) {
            throw new DuplicateResourceException("Product name already exists");
        }

        String sku = resolveSku(request.getSku(), currentUser);

        if (productRepository.existsBySkuAndUser(sku, currentUser)) {
            throw new DuplicateResourceException("Product SKU already exists");
        }

        Category category = resolveCategory(request.getCategoryId(), currentUser);

        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .sku(sku)
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

    public ProductImportResponse importProducts(MultipartFile file, ProductImportMode mode) {
        List<String> errors = new ArrayList<>();
        int createdCount = 0;
        int updatedCount = 0;

        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(inputStream)) {

            Sheet sheet = workbook.getSheetAt(0);

            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);

                if (row == null || isRowEmpty(row)) {
                    continue;
                }

                try {
                    ProductRequest request = ProductRequest.builder()
                            .name(getStringCellValue(row.getCell(0)))
                            .sku(getStringCellValue(row.getCell(1)))
                            .price(getBigDecimalCellValue(row.getCell(2)))
                            .stockQuantity(getIntegerCellValue(row.getCell(3)))
                            .categoryId(resolveCategoryIdFromName(getStringCellValue(row.getCell(4))))
                            .lowStockThreshold(getOptionalIntegerCellValue(row.getCell(5)))
                            .description(getStringCellValue(row.getCell(6)))
                            .build();

                    if (mode == ProductImportMode.CREATE_OR_UPDATE) {
                        boolean updated = updateExistingProductIfPresent(request);

                        if (updated) {
                            updatedCount++;
                        } else {
                            createProduct(request);
                            createdCount++;
                        }
                    } else {
                        createProduct(request);
                        createdCount++;
                    }
                } catch (Exception exception) {
                    errors.add("Row " + (rowIndex + 1) + ": " + exception.getMessage());
                }
            }

        } catch (Exception exception) {
            errors.add("Could not process file: " + exception.getMessage());
        }

        return ProductImportResponse.builder()
                .createdCount(createdCount)
                .updatedCount(updatedCount)
                .failedCount(errors.size())
                .errors(errors)
                .build();
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

        if (productRepository.existsByNameIgnoreCaseAndUserAndIdNot(
                request.getName(),
                currentUser,
                product.getId()
        )) {
            throw new DuplicateResourceException("Product name already exists");
        }

        String sku = resolveSku(request.getSku(), currentUser);

        if (!product.getSku().equals(sku)
                && productRepository.existsBySkuAndUser(sku, currentUser)) {
            throw new DuplicateResourceException("Product SKU already exists");
        }

        Category category = resolveCategory(request.getCategoryId(), currentUser);

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setSku(sku);
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