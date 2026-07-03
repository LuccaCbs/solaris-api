package com.luccavergara.solaris.service;

import com.luccavergara.solaris.entity.*;
import com.luccavergara.solaris.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TenantQueryService {

    private final TenantScopeService tenantScopeService;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final SupplierRepository supplierRepository;
    private final CustomerRepository customerRepository;
    private final SaleRepository saleRepository;
    private final StockMovementRepository stockMovementRepository;
    private final SupplierOrderRepository supplierOrderRepository;
    private final SystemSettingsRepository systemSettingsRepository;
    private final CashRegisterSessionRepository cashRegisterSessionRepository;
    private final AuditLogRepository auditLogRepository;
    private final OrganizationMemberRepository organizationMemberRepository;
    private final FiscalDocumentRepository fiscalDocumentRepository;

    public Optional<Product> findProductById(Long id) {
        User user = tenantScopeService.getCurrentUser();
        return tenantScopeService.resolveOrganizationId(user)
                .flatMap(orgId -> productRepository.findByIdAndOrganizationId(id, orgId))
                .or(() -> productRepository.findByIdAndUser(id, user));
    }

    public List<Product> findAllProducts() {
        User user = tenantScopeService.getCurrentUser();
        return tenantScopeService.resolveOrganizationId(user)
                .map(productRepository::findAllByOrganizationId)
                .orElseGet(() -> productRepository.findAllByUser(user));
    }

    public List<Product> searchProducts(String search) {
        User user = tenantScopeService.getCurrentUser();
        Optional<Long> organizationId = tenantScopeService.resolveOrganizationId(user);

        if (organizationId.isPresent()) {
            Long orgId = organizationId.get();
            return productRepository
                    .findByOrganizationIdAndNameContainingIgnoreCaseOrOrganizationIdAndBarcodeContainingIgnoreCaseOrOrganizationIdAndDescriptionContainingIgnoreCase(
                            orgId,
                            search,
                            orgId,
                            search,
                            orgId,
                            search
                    );
        }

        return productRepository
                .findByUserAndNameContainingIgnoreCaseOrUserAndBarcodeContainingIgnoreCaseOrUserAndDescriptionContainingIgnoreCase(
                        user,
                        search,
                        user,
                        search,
                        user,
                        search
                );
    }

    public Optional<Product> findProductByBarcode(String barcode) {
        User user = tenantScopeService.getCurrentUser();
        return tenantScopeService.resolveOrganizationId(user)
                .flatMap(orgId -> productRepository.findByBarcodeAndOrganizationId(barcode, orgId))
                .or(() -> productRepository.findByBarcodeAndUser(barcode, user));
    }

    public List<Product> findProductsByBarcodePrefix(String prefix) {
        User user = tenantScopeService.getCurrentUser();
        return tenantScopeService.resolveOrganizationId(user)
                .map(orgId -> productRepository.findByOrganizationIdAndBarcodeStartingWith(orgId, prefix))
                .orElseGet(() -> productRepository.findByUserAndBarcodeStartingWith(user, prefix));
    }

    public boolean existsProductByBarcode(String barcode) {
        User user = tenantScopeService.getCurrentUser();
        return tenantScopeService.resolveOrganizationId(user)
                .map(orgId -> productRepository.existsByBarcodeAndOrganizationId(barcode, orgId))
                .orElseGet(() -> productRepository.existsByBarcodeAndUser(barcode, user));
    }

    public boolean existsProductByName(String name) {
        User user = tenantScopeService.getCurrentUser();
        return tenantScopeService.resolveOrganizationId(user)
                .map(orgId -> productRepository.existsByNameIgnoreCaseAndOrganizationId(name, orgId))
                .orElseGet(() -> productRepository.existsByNameIgnoreCaseAndUser(name, user));
    }

    public boolean existsProductByNameExcludingId(String name, Long id) {
        User user = tenantScopeService.getCurrentUser();
        return tenantScopeService.resolveOrganizationId(user)
                .map(orgId -> productRepository.existsByNameIgnoreCaseAndOrganizationIdAndIdNot(name, orgId, id))
                .orElseGet(() -> productRepository.existsByNameIgnoreCaseAndUserAndIdNot(name, user, id));
    }

    public Optional<Product> findProductByNameIgnoreCase(String name) {
        User user = tenantScopeService.getCurrentUser();
        return tenantScopeService.resolveOrganizationId(user)
                .flatMap(orgId -> productRepository.findByNameIgnoreCaseAndOrganizationId(name, orgId))
                .or(() -> productRepository.findByNameIgnoreCaseAndUser(name, user));
    }

    public List<Category> findAllCategories() {
        User user = tenantScopeService.getCurrentUser();
        return tenantScopeService.resolveOrganizationId(user)
                .map(categoryRepository::findAllByOrganizationId)
                .orElseGet(() -> categoryRepository.findAllByUser(user));
    }

    public Optional<Category> findCategoryById(Long id) {
        User user = tenantScopeService.getCurrentUser();
        return tenantScopeService.resolveOrganizationId(user)
                .flatMap(orgId -> categoryRepository.findByIdAndOrganizationId(id, orgId))
                .or(() -> categoryRepository.findByIdAndUser(id, user));
    }

    public Optional<Category> findCategoryByNameIgnoreCase(String name) {
        User user = tenantScopeService.getCurrentUser();
        return tenantScopeService.resolveOrganizationId(user)
                .flatMap(orgId -> categoryRepository.findByNameIgnoreCaseAndOrganizationId(name, orgId))
                .or(() -> categoryRepository.findByNameIgnoreCaseAndUser(name, user));
    }

    public Optional<Category> findCategoryByNameIgnoreCase(User user, String name) {
        return tenantScopeService.resolveOrganizationId(user)
                .flatMap(orgId -> categoryRepository.findByNameIgnoreCaseAndOrganizationId(name, orgId))
                .or(() -> categoryRepository.findByNameIgnoreCaseAndUser(name, user));
    }

    public boolean existsCategoryByName(String name) {
        User user = tenantScopeService.getCurrentUser();
        return tenantScopeService.resolveOrganizationId(user)
                .map(orgId -> categoryRepository.existsByNameIgnoreCaseAndOrganizationId(name, orgId))
                .orElseGet(() -> categoryRepository.existsByNameIgnoreCaseAndUser(name, user));
    }

    public List<Supplier> findAllSuppliers() {
        User user = tenantScopeService.getCurrentUser();
        return tenantScopeService.resolveOrganizationId(user)
                .map(supplierRepository::findAllByOrganizationIdOrderByCreatedAtDesc)
                .orElseGet(() -> supplierRepository.findAllByUserOrderByCreatedAtDesc(user));
    }

    public Optional<Supplier> findSupplierById(Long id) {
        User user = tenantScopeService.getCurrentUser();
        return tenantScopeService.resolveOrganizationId(user)
                .flatMap(orgId -> supplierRepository.findByIdAndOrganizationId(id, orgId))
                .or(() -> supplierRepository.findByIdAndUser(id, user));
    }

    public List<Customer> findAllCustomers() {
        User user = tenantScopeService.getCurrentUser();
        return tenantScopeService.resolveOrganizationId(user)
                .map(customerRepository::findAllByOrganizationIdOrderByCreatedAtDesc)
                .orElseGet(() -> customerRepository.findAllByUserOrderByCreatedAtDesc(user));
    }

    public Optional<Customer> findCustomerById(Long id) {
        User user = tenantScopeService.getCurrentUser();
        return tenantScopeService.resolveOrganizationId(user)
                .flatMap(orgId -> customerRepository.findByIdAndOrganizationId(id, orgId))
                .or(() -> customerRepository.findByIdAndUser(id, user));
    }

    public List<Sale> findAllSales() {
        User user = tenantScopeService.getCurrentUser();
        return tenantScopeService.resolveOrganizationId(user)
                .map(saleRepository::findAllByOrganizationIdOrderByCreatedAtDesc)
                .orElseGet(() -> saleRepository.findAllByUserOrderByCreatedAtDesc(user));
    }

    public Optional<Sale> findSaleById(Long id) {
        User user = tenantScopeService.getCurrentUser();
        return tenantScopeService.resolveOrganizationId(user)
                .flatMap(orgId -> saleRepository.findByIdAndOrganizationId(id, orgId))
                .or(() -> saleRepository.findByIdAndUser(id, user));
    }

    public List<Sale> findSalesBetween(LocalDateTime start, LocalDateTime end) {
        User user = tenantScopeService.getCurrentUser();
        return tenantScopeService.resolveOrganizationId(user)
                .map(orgId -> saleRepository.findByOrganizationIdAndCreatedAtBetweenOrderByCreatedAtDesc(orgId, start, end))
                .orElseGet(() -> saleRepository.findByUserAndCreatedAtBetweenOrderByCreatedAtDesc(user, start, end));
    }

    public List<Sale> findSalesByCashRegisterSessionId(Long cashRegisterSessionId) {
        User user = tenantScopeService.getCurrentUser();
        return tenantScopeService.resolveOrganizationId(user)
                .map(orgId -> saleRepository.findAllByCashRegisterSessionIdAndOrganizationId(cashRegisterSessionId, orgId))
                .orElseGet(() -> saleRepository.findAllByCashRegisterSessionIdAndUser(cashRegisterSessionId, user));
    }

    public List<StockMovement> findAllStockMovements() {
        User user = tenantScopeService.getCurrentUser();
        return tenantScopeService.resolveOrganizationId(user)
                .map(stockMovementRepository::findAllByOrganizationIdOrderByCreatedAtDesc)
                .orElseGet(() -> stockMovementRepository.findAllByUserOrderByCreatedAtDesc(user));
    }

    public List<StockMovement> findStockMovementsByProductId(Long productId) {
        User user = tenantScopeService.getCurrentUser();
        return tenantScopeService.resolveOrganizationId(user)
                .map(orgId -> stockMovementRepository.findByProductIdAndOrganizationIdOrderByCreatedAtDesc(productId, orgId))
                .orElseGet(() -> stockMovementRepository.findByProductIdAndUserOrderByCreatedAtDesc(productId, user));
    }

    public List<SupplierOrder> findAllSupplierOrders() {
        User user = tenantScopeService.getCurrentUser();
        return tenantScopeService.resolveOrganizationId(user)
                .map(supplierOrderRepository::findAllByOrganizationId)
                .orElseGet(() -> supplierOrderRepository.findAllByUser(user));
    }

    public List<FiscalDocument> findAllFiscalDocuments() {
        User user = tenantScopeService.getCurrentUser();
        return tenantScopeService.resolveOrganizationId(user)
                .map(fiscalDocumentRepository::findAllByOrganizationIdOrderByCreatedAtDesc)
                .orElseGet(List::of);
    }

    public Optional<SupplierOrder> findSupplierOrderById(Long id) {
        User user = tenantScopeService.getCurrentUser();
        return tenantScopeService.resolveOrganizationId(user)
                .flatMap(orgId -> supplierOrderRepository.findByIdAndOrganizationId(id, orgId))
                .or(() -> supplierOrderRepository.findByIdAndUser(id, user));
    }

    public Optional<SystemSettings> findSystemSettings() {
        User user = tenantScopeService.getCurrentUser();
        return tenantScopeService.resolveOrganizationId(user)
                .flatMap(systemSettingsRepository::findByOrganizationId)
                .or(() -> systemSettingsRepository.findByUser(user));
    }

    public Optional<SystemSettings> findSystemSettings(User user) {
        return tenantScopeService.resolveOrganizationId(user)
                .flatMap(systemSettingsRepository::findByOrganizationId)
                .or(() -> systemSettingsRepository.findByUser(user));
    }

    public Optional<CashRegisterSession> findOpenCashRegisterSession() {
        User user = tenantScopeService.getCurrentUser();
        return resolveScopedCashRegisterQuery(user,
                () -> cashRegisterSessionRepository.findFirstByStatusAndUserOrderByOpenedAtDesc(
                        CashRegisterStatus.OPEN,
                        user
                ),
                (orgId, storeId) -> cashRegisterSessionRepository.findFirstByStatusAndOrganizationIdAndStoreIdOrderByOpenedAtDesc(
                        CashRegisterStatus.OPEN,
                        orgId,
                        storeId
                ));
    }

    public Optional<CashRegisterSession> findOpenCashRegisterSession(User user) {
        return resolveScopedCashRegisterQuery(user,
                () -> cashRegisterSessionRepository.findFirstByStatusAndUserOrderByOpenedAtDesc(
                        CashRegisterStatus.OPEN,
                        user
                ),
                (orgId, storeId) -> cashRegisterSessionRepository.findFirstByStatusAndOrganizationIdAndStoreIdOrderByOpenedAtDesc(
                        CashRegisterStatus.OPEN,
                        orgId,
                        storeId
                ));
    }

    public Optional<CashRegisterSession> findCashRegisterSessionForDay(LocalDateTime start, LocalDateTime end) {
        User user = tenantScopeService.getCurrentUser();
        return resolveScopedCashRegisterQuery(user,
                () -> cashRegisterSessionRepository.findFirstByUserAndOpenedAtBetweenOrderByOpenedAtDesc(
                        user,
                        start,
                        end
                ),
                (orgId, storeId) -> cashRegisterSessionRepository.findFirstByOrganizationIdAndStoreIdAndOpenedAtBetweenOrderByOpenedAtDesc(
                        orgId,
                        storeId,
                        start,
                        end
                ));
    }

    public Optional<CashRegisterSession> findCashRegisterSessionById(Long id) {
        User user = tenantScopeService.getCurrentUser();
        return resolveScopedCashRegisterQuery(user,
                () -> cashRegisterSessionRepository.findByIdAndUser(id, user),
                (orgId, storeId) -> cashRegisterSessionRepository.findByIdAndOrganizationIdAndStoreId(
                        id,
                        orgId,
                        storeId
                ));
    }

    private Optional<CashRegisterSession> resolveScopedCashRegisterQuery(
            User user,
            java.util.function.Supplier<Optional<CashRegisterSession>> soloUserQuery,
            java.util.function.BiFunction<Long, Long, Optional<CashRegisterSession>> organizationStoreQuery
    ) {
        Optional<Long> organizationId = tenantScopeService.resolveOrganizationId(user);
        if (organizationId.isEmpty()) {
            return soloUserQuery.get();
        }

        Long storeId = tenantScopeService.resolveStoreId(user)
                .orElseThrow(() -> new IllegalStateException("No store assigned for cash register"));

        return organizationStoreQuery.apply(organizationId.get(), storeId);
    }

    public List<AuditLog> findAuditLogs() {
        User user = tenantScopeService.getCurrentUser();
        Optional<Long> organizationId = tenantScopeService.resolveOrganizationId(user);

        if (organizationId.isPresent()) {
            List<Long> userIds = organizationMemberRepository.findUserIdsByOrganizationIdAndStatus(
                    organizationId.get(),
                    OrganizationMemberStatus.ACTIVE
            );

            if (userIds.isEmpty()) {
                return List.of();
            }

            return auditLogRepository.findAllByUserIdInOrderByCreatedAtDesc(userIds);
        }

        return auditLogRepository.findAllByUserIdOrderByCreatedAtDesc(user.getId());
    }
}
