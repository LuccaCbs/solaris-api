package com.luccavergara.solaris.service;

import com.luccavergara.solaris.dto.ProductRequest;
import com.luccavergara.solaris.dto.ProductResponse;
import com.luccavergara.solaris.dto.ProductPreviewResponse;
import com.luccavergara.solaris.dto.StockMovementResponse;
import com.luccavergara.solaris.dto.ProductUpdateRequest;
import com.luccavergara.solaris.entity.Category;
import com.luccavergara.solaris.entity.Product;
import com.luccavergara.solaris.entity.ProductIvaRate;
import com.luccavergara.solaris.entity.BarcodeFormat;
import com.luccavergara.solaris.util.BarcodeUtils;
import com.luccavergara.solaris.entity.User;
import com.luccavergara.solaris.exception.DuplicateResourceException;
import com.luccavergara.solaris.exception.ResourceNotFoundException;
import com.luccavergara.solaris.entity.StockMovementType;
import com.luccavergara.solaris.repository.CategoryRepository;
import com.luccavergara.solaris.repository.ProductRepository;
import com.luccavergara.solaris.repository.SaleItemRepository;
import com.luccavergara.solaris.repository.StockMovementRepository;
import com.luccavergara.solaris.repository.SupplierOrderItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.luccavergara.solaris.dto.ProductImportMode;
import com.luccavergara.solaris.entity.AuditAction;
import com.luccavergara.solaris.entity.AuditEntityType;


import com.luccavergara.solaris.dto.ProductImportResponse;
import org.apache.poi.ss.usermodel.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final CategoryService categoryService;
    private final SystemSettingsService systemSettingsService;
    private final AuthenticatedUserService authenticatedUserService;
    private final AuditLogService auditLogService;
    private final TenantQueryService tenantQueryService;
    private final TenantScopeService tenantScopeService;
    private final StockMovementService stockMovementService;
    private final SaleItemRepository saleItemRepository;
    private final SupplierOrderItemRepository supplierOrderItemRepository;
    private final StockMovementRepository stockMovementRepository;

    private static final String PRODUCT_CREATION_REASON = "Product creation";

    public byte[] generateImportTemplate() {
        try (Workbook workbook = WorkbookFactory.create(true);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Products");

            Row header = sheet.createRow(0);

            String[] headers = {
                    "Name",
                    "Barcode",
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
        User currentUser = authenticatedUserService.getCurrentUser();

        if (categoryName == null || categoryName.isBlank()) {
            return categoryService.getOrCreateDefaultCategory(currentUser).getId();
        }

        Category category = tenantQueryService.findCategoryByNameIgnoreCase(categoryName.trim())
                .filter(value -> isCategoryAccessible(value, currentUser))
                .orElseGet(() -> {
                    Category newCategory = Category.builder()
                            .name(categoryName.trim())
                            .description("")
                            .createdAt(LocalDateTime.now())
                            .systemCategory(false)
                            .user(currentUser)
                            .build();
                    tenantScopeService.getOrganizationReference(currentUser)
                            .ifPresent(newCategory::setOrganization);
                    return categoryRepository.save(newCategory);
                });

        return category.getId();
    }

    private ResolvedBarcode resolveBarcode(String requestedBarcode, BarcodeFormat requestedFormat, User currentUser) {
        if (requestedBarcode != null && !requestedBarcode.isBlank()) {
            String barcode = requestedBarcode.trim();
            BarcodeFormat format = requestedFormat != null
                    ? requestedFormat
                    : BarcodeUtils.detectFormat(barcode);
            BarcodeUtils.validateBarcode(barcode, format);

            return new ResolvedBarcode(barcode, format);
        }

        return generateAutomaticBarcode(currentUser);
    }

    private ResolvedBarcode generateAutomaticBarcode(User currentUser) {
        int maxSequence = productRepository.findByBarcodeStartingWith("779999")
                .stream()
                .map(Product::getBarcode)
                .filter(value -> value.startsWith("779999") && value.length() == 13)
                .map(value -> value.substring(6, 12))
                .filter(value -> value.matches("\\d+"))
                .mapToInt(Integer::parseInt)
                .max()
                .orElse(0);

        for (int offset = 1; offset <= 1000; offset++) {
            String barcode = BarcodeUtils.generateInternalEan13(maxSequence + offset);

            if (!isBarcodeTaken(barcode, currentUser)) {
                return new ResolvedBarcode(barcode, BarcodeFormat.EAN_13);
            }
        }

        throw new IllegalStateException("Could not generate a unique internal barcode");
    }

    private boolean isBarcodeTaken(String barcode, User currentUser) {
        if (productRepository.existsByBarcode(barcode)) {
            return true;
        }

        return tenantQueryService.existsProductByBarcode(barcode);
    }

    private record ResolvedBarcode(String value, BarcodeFormat format) {
    }

    private Category resolveCategory(Long categoryId, User currentUser) {
        if (categoryId == null) {
            throw new IllegalArgumentException("Category is required");
        }

        Category category = tenantQueryService.findCategoryById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        if (!isCategoryAccessible(category, currentUser)) {
            throw new ResourceNotFoundException("Category not found");
        }

        return category;
    }

    private boolean isCategoryAccessible(Category category, User currentUser) {
        return tenantScopeService.resolveOrganizationId(currentUser)
                .map(orgId -> category.getOrganization() != null && orgId.equals(category.getOrganization().getId()))
                .orElseGet(() -> category.getUser().getId().equals(currentUser.getId()));
    }

    private boolean updateExistingProductIfPresent(ProductRequest request) {
        User currentUser = authenticatedUserService.getCurrentUser();

        Product existingProduct = tenantQueryService.findProductByNameIgnoreCase(request.getName())
                .orElse(null);

        if (existingProduct == null) {
            return false;
        }

        ResolvedBarcode barcode = resolveBarcodeForImportUpdate(
                request.getBarcode(),
                request.getBarcodeFormat(),
                existingProduct,
                currentUser
        );

        if (!existingProduct.getBarcode().equals(barcode.value())
                && isBarcodeTaken(barcode.value(), currentUser)) {
            throw new DuplicateResourceException("Product barcode already exists");
        }

        Category category = resolveCategory(request.getCategoryId(), currentUser);

        existingProduct.setDescription(request.getDescription());
        existingProduct.setBarcode(barcode.value());
        existingProduct.setBarcodeFormat(barcode.format());
        existingProduct.setPrice(request.getPrice());
        existingProduct.setStockQuantity(request.getStockQuantity());
        existingProduct.setCategory(category);
        existingProduct.setLowStockThreshold(request.getLowStockThreshold());
        existingProduct.setIvaRate(resolveIvaRate(request.getIvaRate()));

        productRepository.save(existingProduct);

        auditLogService.log(
                AuditAction.CREATE,
                AuditEntityType.PRODUCT,
                existingProduct.getId(),
                existingProduct.getName(),
                "Product updated by import: " + existingProduct.getName()
        );

        return true;
    }

    private ResolvedBarcode resolveBarcodeForImportUpdate(
            String requestedBarcode,
            BarcodeFormat requestedFormat,
            Product existingProduct,
            User currentUser
    ) {
        if (requestedBarcode != null && !requestedBarcode.isBlank()) {
            return resolveBarcode(requestedBarcode, requestedFormat, currentUser);
        }

        return new ResolvedBarcode(existingProduct.getBarcode(), existingProduct.getBarcodeFormat());
    }

    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        User currentUser = authenticatedUserService.getCurrentUser();

        if (tenantQueryService.existsProductByName(request.getName())) {
            throw new DuplicateResourceException("Product name already exists");
        }

        ResolvedBarcode barcode = resolveBarcode(
                request.getBarcode(),
                request.getBarcodeFormat(),
                currentUser
        );

        if (isBarcodeTaken(barcode.value(), currentUser)) {
            throw new DuplicateResourceException("Product barcode already exists");
        }

        Category category = resolveCategory(request.getCategoryId(), currentUser);

        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .barcode(barcode.value())
                .barcodeFormat(barcode.format())
                .price(request.getPrice())
                .stockQuantity(request.getStockQuantity())
                .lowStockThreshold(request.getLowStockThreshold())
                .createdAt(LocalDateTime.now())
                .category(category)
                .user(currentUser)
                .active(true)
                .ivaRate(resolveIvaRate(request.getIvaRate()))
                .build();

        tenantScopeService.getOrganizationReference(currentUser)
                .ifPresent(organization -> {
                    product.setOrganization(organization);
                    product.setCreatedBy(currentUser);
                });

        Product savedProduct = productRepository.save(product);

        auditLogService.log(
                AuditAction.CREATE,
                AuditEntityType.PRODUCT,
                savedProduct.getId(),
                savedProduct.getName(),
                "Product created"
        );

        if (savedProduct.getStockQuantity() != null && savedProduct.getStockQuantity() > 0) {
            stockMovementService.recordInitialStockMovement(savedProduct, PRODUCT_CREATION_REASON);
        }

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
                            .barcode(getStringCellValue(row.getCell(1)))
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

    public List<ProductResponse> getAllProducts(String search, Boolean active) {
        User currentUser = authenticatedUserService.getCurrentUser();

        List<Product> products;

        if (search == null || search.isBlank()) {
            products = tenantQueryService.findAllProducts();
        } else {
            products = tenantQueryService.searchProducts(search);
        }

        return products.stream()
                .filter(product -> active == null || product.getActive().equals(active))
                .map(this::mapToResponse)
                .toList();
    }

    public ProductResponse getProductByBarcode(String barcode) {
        Product product = tenantQueryService.findProductByBarcode(barcode.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        if (Boolean.FALSE.equals(product.getActive())) {
            throw new ResourceNotFoundException("Product not found");
        }

        return mapToResponse(product);
    }

    public ProductResponse getProductById(Long id) {
        User currentUser = authenticatedUserService.getCurrentUser();

        Product product = tenantQueryService.findProductById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        return mapToResponse(product);
    }

    public ProductResponse updateProduct(Long id, ProductUpdateRequest request) {
        User currentUser = authenticatedUserService.getCurrentUser();

        Product product = tenantQueryService.findProductById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        if (tenantQueryService.existsProductByNameExcludingId(
                request.getName(),
                product.getId()
        )) {
            throw new DuplicateResourceException("Product name already exists");
        }

        ResolvedBarcode barcode = resolveBarcode(
                request.getBarcode(),
                request.getBarcodeFormat(),
                currentUser
        );

        if (!product.getBarcode().equals(barcode.value())
                && isBarcodeTaken(barcode.value(), currentUser)) {
            throw new DuplicateResourceException("Product barcode already exists");
        }

        Category category = resolveCategory(request.getCategoryId(), currentUser);

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setBarcode(barcode.value());
        product.setBarcodeFormat(barcode.format());
        product.setPrice(request.getPrice());
        product.setCategory(category);
        product.setLowStockThreshold(request.getLowStockThreshold());
        product.setIvaRate(resolveIvaRate(request.getIvaRate()));

        Product updatedProduct = productRepository.save(product);

        auditLogService.log(
                AuditAction.CREATE,
                AuditEntityType.PRODUCT,
                updatedProduct.getId(),
                updatedProduct.getName(),
                "Product updated: " + updatedProduct.getName()
        );

        return mapToResponse(updatedProduct);
    }

    public ProductResponse deactivateProduct(Long id) {
        User currentUser = authenticatedUserService.getCurrentUser();

        Product product = tenantQueryService.findProductById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        product.setActive(false);

        Product updatedProduct = productRepository.save(product);

        auditLogService.log(
                AuditAction.CREATE,
                AuditEntityType.PRODUCT,
                updatedProduct.getId(),
                updatedProduct.getName(),
                "Product deactivated: " + updatedProduct.getName()
        );

        return mapToResponse(updatedProduct);
    }

    public ProductResponse activateProduct(Long id) {
        User currentUser = authenticatedUserService.getCurrentUser();

        Product product = tenantQueryService.findProductById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        product.setActive(true);

        Product updatedProduct = productRepository.save(product);

        auditLogService.log(
                AuditAction.CREATE,
                AuditEntityType.PRODUCT,
                updatedProduct.getId(),
                updatedProduct.getName(),
                "Product activated: " + updatedProduct.getName()
        );

        return mapToResponse(updatedProduct);
    }

    public ProductPreviewResponse getProductPreview(Long id) {
        Product product = tenantQueryService.findProductById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        User currentUser = authenticatedUserService.getCurrentUser();
        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime monthEnd = monthStart.plusMonths(1);

        Integer salesQuantityThisMonth;
        java.math.BigDecimal salesRevenueThisMonth;

        if (tenantScopeService.resolveOrganizationId(currentUser).isPresent()) {
            Long organizationId = tenantScopeService.resolveOrganizationId(currentUser).get();
            salesQuantityThisMonth = saleItemRepository.sumQuantityByProductAndOrganizationAndDateRange(
                    id, organizationId, monthStart, monthEnd
            );
            salesRevenueThisMonth = saleItemRepository.sumRevenueByProductAndOrganizationAndDateRange(
                    id, organizationId, monthStart, monthEnd
            );
        } else {
            salesQuantityThisMonth = saleItemRepository.sumQuantityByProductAndUserAndDateRange(
                    id, currentUser.getId(), monthStart, monthEnd
            );
            salesRevenueThisMonth = saleItemRepository.sumRevenueByProductAndUserAndDateRange(
                    id, currentUser.getId(), monthStart, monthEnd
            );
        }

        long supplierOrderAppearances = supplierOrderItemRepository.countByProductId(id);
        long restockCount = stockMovementRepository.countByProductIdAndType(id, StockMovementType.IN);

        List<StockMovementResponse> recentStockMovements = stockMovementService.getMovementsByProduct(id)
                .stream()
                .limit(10)
                .toList();

        return ProductPreviewResponse.builder()
                .product(mapToResponse(product))
                .salesQuantityThisMonth(salesQuantityThisMonth)
                .salesRevenueThisMonth(salesRevenueThisMonth)
                .supplierOrderAppearances(supplierOrderAppearances)
                .restockCount(restockCount)
                .recentStockMovements(recentStockMovements)
                .build();
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
                .barcode(product.getBarcode())
                .barcodeFormat(product.getBarcodeFormat())
                .price(product.getPrice())
                .stockQuantity(product.getStockQuantity())
                .lowStockThreshold(product.getLowStockThreshold())
                .effectiveLowStockThreshold(effectiveLowStockThreshold)
                .lowStock(lowStock)
                .createdAt(product.getCreatedAt())
                .categoryId(product.getCategory() != null ? product.getCategory().getId() : null)
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                .active(product.getActive())
                .ivaRate(product.getIvaRate())
                .build();
    }

    private ProductIvaRate resolveIvaRate(ProductIvaRate ivaRate) {
        return ivaRate != null ? ivaRate : ProductIvaRate.GENERAL_21;
    }
}