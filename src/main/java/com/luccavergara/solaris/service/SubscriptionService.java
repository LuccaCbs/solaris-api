package com.luccavergara.solaris.service;

import com.luccavergara.solaris.billing.BillingPricingService;
import com.luccavergara.solaris.billing.PaymentProvider;
import com.luccavergara.solaris.billing.PaymentProviderFactory;
import com.luccavergara.solaris.dto.OrganizationEntitlementsResponse;
import com.luccavergara.solaris.dto.OrganizationSubscriptionResponse;
import com.luccavergara.solaris.dto.StoreAddonCheckoutRequest;
import com.luccavergara.solaris.dto.StoreAddonCheckoutResponse;
import com.luccavergara.solaris.entity.*;
import com.luccavergara.solaris.exception.ResourceNotFoundException;
import com.luccavergara.solaris.exception.SubscriptionLimitException;
import com.luccavergara.solaris.repository.OrganizationRepository;
import com.luccavergara.solaris.repository.OrganizationSubscriptionRepository;
import com.luccavergara.solaris.repository.StoreRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class SubscriptionService {

    private static final int TRIAL_DAYS = 14;

    private final OrganizationRepository organizationRepository;
    private final OrganizationSubscriptionRepository subscriptionRepository;
    private final StoreRepository storeRepository;
    private final PaymentProviderFactory paymentProviderFactory;
    private final BillingPricingService billingPricingService;
    private final EntitlementService entitlementService;

    @Value("${application.billing.mock-enabled:true}")
    private boolean billingMockEnabled;

    public SubscriptionService(
            OrganizationRepository organizationRepository,
            OrganizationSubscriptionRepository subscriptionRepository,
            StoreRepository storeRepository,
            @Lazy PaymentProviderFactory paymentProviderFactory,
            BillingPricingService billingPricingService,
            EntitlementService entitlementService
    ) {
        this.organizationRepository = organizationRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.storeRepository = storeRepository;
        this.paymentProviderFactory = paymentProviderFactory;
        this.billingPricingService = billingPricingService;
        this.entitlementService = entitlementService;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public OrganizationSubscription ensureSubscription(Organization organization) {
        return subscriptionRepository.findByOrganization(organization)
                .orElseGet(() -> createDefaultSubscription(organization));
    }

    @Transactional(readOnly = true)
    public OrganizationSubscriptionResponse getSubscription(Long organizationId) {
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));

        OrganizationSubscription subscription = ensureSubscription(organization);
        long activeStoreCount = storeRepository.countByOrganizationIdAndActiveTrue(organizationId);

        return mapToResponse(subscription, activeStoreCount);
    }

    @Transactional(readOnly = true)
    public void assertCanCreateStore(Long organizationId) {
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));

        OrganizationSubscription subscription = ensureSubscription(organization);

        if (!subscription.isBillingActive()) {
            throw new SubscriptionLimitException(
                    "Your subscription is not active. Update billing to add more stores."
            );
        }

        long activeStoreCount = storeRepository.countByOrganizationIdAndActiveTrue(organizationId);

        if (activeStoreCount >= subscription.getAllowedStoreCount()) {
            throw new SubscriptionLimitException(
                    "Store limit reached. Purchase an additional store slot to continue."
            );
        }

        if (activeStoreCount >= 1) {
            entitlementService.assertModule(organizationId, ModuleCode.MULTI_STORE);
        }
    }

    @Transactional(readOnly = true)
    public StoreAddonCheckoutResponse initiateStoreAddonCheckout(
            Long organizationId,
            StoreAddonCheckoutRequest request
    ) {
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));

        ensureSubscription(organization);

        int quantity = request.getQuantity() != null ? request.getQuantity() : 1;
        PaymentProvider paymentProvider = paymentProviderFactory.resolve(organization);
        var price = billingPricingService.resolveStoreAddonPrice(organization);

        if (paymentProvider.isConfigured()) {
            return paymentProvider.createStoreAddonCheckout(organization, quantity);
        }

        return StoreAddonCheckoutResponse.builder()
                .status("PAYMENT_REQUIRED")
                .message(paymentProvider.notConfiguredMessage())
                .checkoutUrl(null)
                .provider(paymentProvider.providerType())
                .providerDisplayName(paymentProvider.displayName())
                .supportedPaymentMethods(paymentProvider.supportedPaymentMethods())
                .quantity(quantity)
                .currency(price.currency())
                .unitPrice(price.unitAmount())
                .unitPriceArs(price.isArs() ? price.unitAmount() : null)
                .mockPurchaseAvailable(billingMockEnabled)
                .build();
    }

    @Transactional
    public OrganizationSubscriptionResponse applyStoreAddonPurchase(Long organizationId, int quantity) {
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));

        return applyStoreAddonPurchase(
                organizationId,
                quantity,
                paymentProviderFactory.resolveProviderType(organization)
        );
    }

    @Transactional
    public OrganizationSubscriptionResponse applyStoreAddonPurchase(
            Long organizationId,
            int quantity,
            BillingProvider billingProvider
    ) {
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));

        OrganizationSubscription subscription = ensureSubscription(organization);

        if (!subscription.isBillingActive()) {
            subscription.setStatus(SubscriptionStatus.ACTIVE);
        }

        subscription.setExtraStoresPurchased(subscription.getExtraStoresPurchased() + quantity);
        subscription.setBillingProvider(billingProvider);
        subscription.setUpdatedAt(LocalDateTime.now());

        OrganizationSubscription saved = subscriptionRepository.save(subscription);
        long activeStoreCount = storeRepository.countByOrganizationIdAndActiveTrue(organizationId);

        return mapToResponse(saved, activeStoreCount);
    }

    @Transactional
    public OrganizationSubscriptionResponse purchaseStoreAddonMock(
            Long organizationId,
            StoreAddonCheckoutRequest request
    ) {
        if (!billingMockEnabled) {
            throw new UnsupportedOperationException("Mock billing is disabled");
        }

        int quantity = request.getQuantity() != null ? request.getQuantity() : 1;
        return applyStoreAddonPurchase(organizationId, quantity);
    }

    private OrganizationSubscription createDefaultSubscription(Organization organization) {
        LocalDateTime now = LocalDateTime.now();

        return subscriptionRepository.save(
                OrganizationSubscription.builder()
                        .organization(organization)
                        .planCode(SubscriptionPlanCode.POS)
                        .status(SubscriptionStatus.TRIALING)
                        .maxStores(1)
                        .extraStoresPurchased(0)
                        .billingProvider(BillingProvider.NONE)
                        .trialEndsAt(now.plusDays(TRIAL_DAYS))
                        .currentPeriodStart(now)
                        .currentPeriodEnd(now.plusDays(TRIAL_DAYS))
                        .createdAt(now)
                        .updatedAt(now)
                        .build()
        );
    }

    private OrganizationSubscriptionResponse mapToResponse(
            OrganizationSubscription subscription,
            long activeStoreCount
    ) {
        int allowedStores = subscription.getAllowedStoreCount();
        boolean hasStoreCapacity = activeStoreCount < allowedStores;
        boolean requiresMultiStoreModule = activeStoreCount >= 1;
        boolean hasMultiStoreModule = entitlementService.hasModule(
                subscription.getOrganization().getId(),
                ModuleCode.MULTI_STORE
        );
        boolean canAddStore = subscription.isBillingActive()
                && hasStoreCapacity
                && (!requiresMultiStoreModule || hasMultiStoreModule);

        OrganizationEntitlementsResponse entitlements = entitlementService.getEntitlements(
                subscription.getOrganization().getId()
        );

        Organization organization = subscription.getOrganization();
        PaymentProvider paymentProvider = paymentProviderFactory.resolve(organization);

        return OrganizationSubscriptionResponse.builder()
                .planCode(subscription.getPlanCode())
                .planDisplayName(entitlementService.resolvePlanDisplayName(subscription.getPlanCode()))
                .status(subscription.getStatus())
                .maxStores(subscription.getMaxStores())
                .extraStoresPurchased(subscription.getExtraStoresPurchased())
                .allowedStores(allowedStores)
                .activeStoreCount(activeStoreCount)
                .canAddStore(canAddStore)
                .billingProvider(subscription.getBillingProvider())
                .preferredBillingProvider(paymentProvider.providerType())
                .paymentProviderDisplayName(paymentProvider.displayName())
                .countryCode(organization.getCountryCode())
                .billingJurisdiction(organization.getBillingJurisdiction())
                .defaultCurrency(organization.getDefaultCurrency())
                .trialEndsAt(subscription.getTrialEndsAt())
                .currentPeriodStart(subscription.getCurrentPeriodStart())
                .currentPeriodEnd(subscription.getCurrentPeriodEnd())
                .activeModules(entitlements.getActiveModules())
                .planModules(entitlements.getPlanModules())
                .addonModules(entitlements.getAddonModules())
                .promoModules(entitlements.getPromoModules())
                .build();
    }
}
