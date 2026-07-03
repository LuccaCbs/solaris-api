package com.luccavergara.solaris.service;

import com.luccavergara.solaris.dto.OrganizationSubscriptionResponse;
import com.luccavergara.solaris.dto.StoreAddonCheckoutRequest;
import com.luccavergara.solaris.dto.StoreAddonCheckoutResponse;
import com.luccavergara.solaris.entity.*;
import com.luccavergara.solaris.exception.ResourceNotFoundException;
import com.luccavergara.solaris.exception.SubscriptionLimitException;
import com.luccavergara.solaris.repository.OrganizationRepository;
import com.luccavergara.solaris.repository.OrganizationSubscriptionRepository;
import com.luccavergara.solaris.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private static final int TRIAL_DAYS = 14;

    private final OrganizationRepository organizationRepository;
    private final OrganizationSubscriptionRepository subscriptionRepository;
    private final StoreRepository storeRepository;

    @Value("${application.billing.mock-enabled:true}")
    private boolean billingMockEnabled;

    @Value("${application.billing.store-addon-price-ars:15000}")
    private BigDecimal storeAddonPriceArs;

    @Value("${application.billing.provider:MERCADOPAGO}")
    private String billingProviderName;

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
        BillingProvider provider = resolveBillingProvider();

        return StoreAddonCheckoutResponse.builder()
                .status("PAYMENT_REQUIRED")
                .message("Payment integration is not configured yet. Complete checkout to unlock additional stores.")
                .checkoutUrl(null)
                .provider(provider)
                .quantity(quantity)
                .unitPriceArs(storeAddonPriceArs)
                .mockPurchaseAvailable(billingMockEnabled)
                .build();
    }

    @Transactional
    public OrganizationSubscriptionResponse purchaseStoreAddonMock(
            Long organizationId,
            StoreAddonCheckoutRequest request
    ) {
        if (!billingMockEnabled) {
            throw new UnsupportedOperationException("Mock billing is disabled");
        }

        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));

        OrganizationSubscription subscription = ensureSubscription(organization);

        if (!subscription.isBillingActive()) {
            subscription.setStatus(SubscriptionStatus.ACTIVE);
        }

        int quantity = request.getQuantity() != null ? request.getQuantity() : 1;
        subscription.setExtraStoresPurchased(subscription.getExtraStoresPurchased() + quantity);
        subscription.setUpdatedAt(LocalDateTime.now());

        OrganizationSubscription saved = subscriptionRepository.save(subscription);
        long activeStoreCount = storeRepository.countByOrganizationIdAndActiveTrue(organizationId);

        return mapToResponse(saved, activeStoreCount);
    }

    private OrganizationSubscription createDefaultSubscription(Organization organization) {
        LocalDateTime now = LocalDateTime.now();

        return subscriptionRepository.save(
                OrganizationSubscription.builder()
                        .organization(organization)
                        .planCode(SubscriptionPlanCode.STARTER)
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
        boolean canAddStore = subscription.isBillingActive() && activeStoreCount < allowedStores;

        return OrganizationSubscriptionResponse.builder()
                .planCode(subscription.getPlanCode())
                .status(subscription.getStatus())
                .maxStores(subscription.getMaxStores())
                .extraStoresPurchased(subscription.getExtraStoresPurchased())
                .allowedStores(allowedStores)
                .activeStoreCount(activeStoreCount)
                .canAddStore(canAddStore)
                .billingProvider(subscription.getBillingProvider())
                .trialEndsAt(subscription.getTrialEndsAt())
                .currentPeriodStart(subscription.getCurrentPeriodStart())
                .currentPeriodEnd(subscription.getCurrentPeriodEnd())
                .build();
    }

    private BillingProvider resolveBillingProvider() {
        try {
            return BillingProvider.valueOf(billingProviderName.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return BillingProvider.MERCADOPAGO;
        }
    }
}
