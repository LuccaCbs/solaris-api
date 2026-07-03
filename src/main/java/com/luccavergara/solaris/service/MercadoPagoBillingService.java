package com.luccavergara.solaris.service;

import com.luccavergara.solaris.billing.MercadoPagoClient;
import com.luccavergara.solaris.dto.StoreAddonCheckoutResponse;
import com.luccavergara.solaris.entity.*;
import com.luccavergara.solaris.exception.ResourceNotFoundException;
import com.luccavergara.solaris.repository.BillingCheckoutRepository;
import com.luccavergara.solaris.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class MercadoPagoBillingService {

    private static final String EXTERNAL_REFERENCE_PREFIX = "solaris:checkout:";

    private final OrganizationRepository organizationRepository;
    private final BillingCheckoutRepository billingCheckoutRepository;
    private final MercadoPagoClient mercadoPagoClient;
    private final SubscriptionService subscriptionService;

    @Value("${application.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    @Value("${application.billing.api-public-url:}")
    private String apiPublicUrl;

    @Value("${application.billing.mercadopago.use-sandbox:false}")
    private boolean useSandbox;

    @Value("${application.billing.store-addon-price-ars:15000}")
    private BigDecimal storeAddonPriceArs;

    public boolean isConfigured() {
        return mercadoPagoClient.isConfigured();
    }

    @Transactional
    public StoreAddonCheckoutResponse createStoreAddonCheckout(Long organizationId, int quantity) {
        if (!isConfigured()) {
            throw new IllegalStateException("Mercado Pago is not configured");
        }

        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));

        subscriptionService.ensureSubscription(organization);

        BigDecimal totalAmount = storeAddonPriceArs.multiply(BigDecimal.valueOf(quantity));
        LocalDateTime now = LocalDateTime.now();

        BillingCheckout checkout = billingCheckoutRepository.save(
                BillingCheckout.builder()
                        .organization(organization)
                        .checkoutType(BillingCheckoutType.STORE_ADDON)
                        .quantity(quantity)
                        .unitAmount(storeAddonPriceArs)
                        .totalAmount(totalAmount)
                        .currency("ARS")
                        .status(BillingCheckoutStatus.PENDING)
                        .provider(BillingProvider.MERCADOPAGO)
                        .createdAt(now)
                        .updatedAt(now)
                        .build()
        );

        String billingReturnUrl = frontendUrl + "/admin/billing";
        MercadoPagoClient.MercadoPagoPreference preference = mercadoPagoClient.createStoreAddonPreference(
                new MercadoPagoClient.CreatePreferenceCommand(
                        "Solaris - Sucursal adicional",
                        "Compra de " + quantity + " sucursal(es) adicional(es) para tu organización",
                        quantity,
                        storeAddonPriceArs,
                        "ARS",
                        buildExternalReference(checkout.getId()),
                        resolveNotificationUrl(),
                        billingReturnUrl + "?status=success",
                        billingReturnUrl + "?status=failure",
                        billingReturnUrl + "?status=pending"
                )
        );

        checkout.setExternalPreferenceId(preference.id());
        checkout.setUpdatedAt(LocalDateTime.now());
        billingCheckoutRepository.save(checkout);

        String checkoutUrl = useSandbox ? preference.sandboxInitPoint() : preference.initPoint();

        return StoreAddonCheckoutResponse.builder()
                .status("READY")
                .message("Redirect the customer to Mercado Pago to complete payment with card.")
                .checkoutUrl(checkoutUrl)
                .provider(BillingProvider.MERCADOPAGO)
                .quantity(quantity)
                .unitPriceArs(storeAddonPriceArs)
                .checkoutId(checkout.getId())
                .preferenceId(preference.id())
                .mockPurchaseAvailable(false)
                .build();
    }

    @Transactional
    public void processPaymentNotification(String paymentId) {
        MercadoPagoClient.MercadoPagoPayment payment = mercadoPagoClient.getPayment(paymentId);

        if (!payment.isApproved()) {
            log.info("Ignoring Mercado Pago payment {} with status {}", payment.id(), payment.status());
            return;
        }

        BillingCheckout checkout = resolveCheckout(payment)
                .orElseThrow(() -> new ResourceNotFoundException("Billing checkout not found for payment"));

        fulfillCheckout(checkout, payment.id());
    }

    @Transactional
    public void fulfillCheckout(BillingCheckout checkout, String paymentId) {
        if (checkout.getStatus() == BillingCheckoutStatus.APPROVED) {
            return;
        }

        checkout.setStatus(BillingCheckoutStatus.APPROVED);
        checkout.setExternalPaymentId(paymentId);
        checkout.setFulfilledAt(LocalDateTime.now());
        checkout.setUpdatedAt(LocalDateTime.now());
        billingCheckoutRepository.save(checkout);

        subscriptionService.applyStoreAddonPurchase(
                checkout.getOrganization().getId(),
                checkout.getQuantity(),
                BillingProvider.MERCADOPAGO
        );
    }

    private java.util.Optional<BillingCheckout> resolveCheckout(MercadoPagoClient.MercadoPagoPayment payment) {
        if (StringUtils.hasText(payment.id())) {
            java.util.Optional<BillingCheckout> byPayment = billingCheckoutRepository
                    .findByExternalPaymentId(payment.id());

            if (byPayment.isPresent()) {
                return byPayment;
            }
        }

        if (StringUtils.hasText(payment.preferenceId())) {
            java.util.Optional<BillingCheckout> byPreference = billingCheckoutRepository
                    .findByExternalPreferenceId(payment.preferenceId());

            if (byPreference.isPresent()) {
                return byPreference;
            }
        }

        Long checkoutId = parseCheckoutId(payment.externalReference());

        if (checkoutId == null) {
            return java.util.Optional.empty();
        }

        return billingCheckoutRepository.findById(checkoutId);
    }

    private Long parseCheckoutId(String externalReference) {
        if (!StringUtils.hasText(externalReference) || !externalReference.startsWith(EXTERNAL_REFERENCE_PREFIX)) {
            return null;
        }

        try {
            return Long.parseLong(externalReference.substring(EXTERNAL_REFERENCE_PREFIX.length()));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String buildExternalReference(Long checkoutId) {
        return EXTERNAL_REFERENCE_PREFIX + checkoutId;
    }

    private String resolveNotificationUrl() {
        if (!StringUtils.hasText(apiPublicUrl)) {
            throw new IllegalStateException("application.billing.api-public-url is required for Mercado Pago webhooks");
        }

        String normalizedBase = apiPublicUrl.endsWith("/")
                ? apiPublicUrl.substring(0, apiPublicUrl.length() - 1)
                : apiPublicUrl;

        return normalizedBase + "/api/v1/billing/mercadopago/webhook";
    }
}
