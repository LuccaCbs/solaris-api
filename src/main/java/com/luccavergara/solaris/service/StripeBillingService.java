package com.luccavergara.solaris.service;

import com.luccavergara.solaris.billing.BillingPricingService;
import com.luccavergara.solaris.dto.StoreAddonCheckoutResponse;
import com.luccavergara.solaris.entity.*;
import com.luccavergara.solaris.exception.ResourceNotFoundException;
import com.luccavergara.solaris.repository.BillingCheckoutRepository;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
@Slf4j
public class StripeBillingService {

    private static final String CHECKOUT_METADATA_KEY = "checkout_id";

    private final BillingCheckoutRepository billingCheckoutRepository;
    private final BillingPricingService billingPricingService;
    private final SubscriptionService subscriptionService;

    public StripeBillingService(
            BillingCheckoutRepository billingCheckoutRepository,
            BillingPricingService billingPricingService,
            @Lazy SubscriptionService subscriptionService
    ) {
        this.billingCheckoutRepository = billingCheckoutRepository;
        this.billingPricingService = billingPricingService;
        this.subscriptionService = subscriptionService;
    }

    @Value("${application.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    @Value("${application.billing.stripe.secret-key:}")
    private String secretKey;

    @Value("${application.billing.stripe.webhook-secret:}")
    private String webhookSecret;

    public boolean isConfigured() {
        return StringUtils.hasText(secretKey);
    }

    @Transactional
    public StoreAddonCheckoutResponse createStoreAddonCheckout(Organization organization, int quantity) {
        if (!isConfigured()) {
            throw new IllegalStateException("Stripe is not configured");
        }

        Stripe.apiKey = secretKey;

        BillingPriceSnapshot price = resolvePrice(organization);
        BigDecimal totalAmount = price.unitAmount().multiply(BigDecimal.valueOf(quantity));
        LocalDateTime now = LocalDateTime.now();

        BillingCheckout checkout = billingCheckoutRepository.save(
                BillingCheckout.builder()
                        .organization(organization)
                        .checkoutType(BillingCheckoutType.STORE_ADDON)
                        .quantity(quantity)
                        .unitAmount(price.unitAmount())
                        .totalAmount(totalAmount)
                        .currency(price.currency())
                        .status(BillingCheckoutStatus.PENDING)
                        .provider(BillingProvider.STRIPE)
                        .createdAt(now)
                        .updatedAt(now)
                        .build()
        );

        String billingReturnUrl = frontendUrl + "/admin/billing";

        try {
            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl(billingReturnUrl + "?status=success")
                    .setCancelUrl(billingReturnUrl + "?status=failure")
                    .putMetadata(CHECKOUT_METADATA_KEY, checkout.getId().toString())
                    .putMetadata("organization_id", organization.getId().toString())
                    .addLineItem(
                            SessionCreateParams.LineItem.builder()
                                    .setQuantity((long) quantity)
                                    .setPriceData(
                                            SessionCreateParams.LineItem.PriceData.builder()
                                                    .setCurrency(price.currency().toLowerCase())
                                                    .setUnitAmount(toMinorUnits(price.unitAmount()))
                                                    .setProductData(
                                                            SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                    .setName("Solaris - Additional store")
                                                                    .setDescription(
                                                                            "Purchase of "
                                                                                    + quantity
                                                                                    + " additional store slot(s)"
                                                                    )
                                                                    .build()
                                                    )
                                                    .build()
                                    )
                                    .build()
                    )
                    .build();

            Session session = Session.create(params);

            checkout.setExternalPreferenceId(session.getId());
            checkout.setUpdatedAt(LocalDateTime.now());
            billingCheckoutRepository.save(checkout);

            return StoreAddonCheckoutResponse.builder()
                    .status("READY")
                    .message(
                            "Redirect the customer to Stripe Checkout. Google Pay and Apple Pay appear automatically when available."
                    )
                    .checkoutUrl(session.getUrl())
                    .provider(BillingProvider.STRIPE)
                    .quantity(quantity)
                    .currency(price.currency())
                    .unitPrice(price.unitAmount())
                    .checkoutId(checkout.getId())
                    .preferenceId(session.getId())
                    .mockPurchaseAvailable(false)
                    .build();
        } catch (StripeException ex) {
            log.error("Failed to create Stripe checkout session: {}", ex.getMessage());
            throw new IllegalStateException("Could not create Stripe checkout session");
        }
    }

    @Transactional
    public void processWebhookPayload(String payload, String signatureHeader) {
        if (!StringUtils.hasText(webhookSecret)) {
            throw new IllegalStateException("Stripe webhook secret is not configured");
        }

        Event event;

        try {
            event = Webhook.constructEvent(payload, signatureHeader, webhookSecret);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid Stripe webhook signature");
        }

        if (!"checkout.session.completed".equals(event.getType())) {
            return;
        }

        Session session = (Session) event.getDataObjectDeserializer()
                .getObject()
                .filter(Session.class::isInstance)
                .map(Session.class::cast)
                .orElse(null);

        if (session == null) {
            log.warn("Stripe webhook checkout.session.completed without session payload");
            return;
        }

        fulfillCheckoutSession(session);
    }

    @Transactional
    public void fulfillCheckoutSession(Session session) {
        if (!"paid".equalsIgnoreCase(session.getPaymentStatus())) {
            log.info("Ignoring Stripe session {} with payment status {}", session.getId(), session.getPaymentStatus());
            return;
        }

        BillingCheckout checkout = resolveCheckout(session)
                .orElseThrow(() -> new ResourceNotFoundException("Billing checkout not found for Stripe session"));

        if (checkout.getStatus() == BillingCheckoutStatus.APPROVED) {
            return;
        }

        checkout.setStatus(BillingCheckoutStatus.APPROVED);
        checkout.setExternalPaymentId(session.getPaymentIntent());
        checkout.setFulfilledAt(LocalDateTime.now());
        checkout.setUpdatedAt(LocalDateTime.now());
        billingCheckoutRepository.save(checkout);

        subscriptionService.applyStoreAddonPurchase(
                checkout.getOrganization().getId(),
                checkout.getQuantity(),
                BillingProvider.STRIPE
        );
    }

    private java.util.Optional<BillingCheckout> resolveCheckout(Session session) {
        if (session.getMetadata() != null && StringUtils.hasText(session.getMetadata().get(CHECKOUT_METADATA_KEY))) {
            try {
                Long checkoutId = Long.parseLong(session.getMetadata().get(CHECKOUT_METADATA_KEY));
                return billingCheckoutRepository.findById(checkoutId);
            } catch (NumberFormatException ignored) {
                // Fall through to external preference lookup.
            }
        }

        if (StringUtils.hasText(session.getId())) {
            return billingCheckoutRepository.findByExternalPreferenceId(session.getId());
        }

        return java.util.Optional.empty();
    }

    private BillingPriceSnapshot resolvePrice(Organization organization) {
        var price = billingPricingService.resolveStoreAddonPrice(organization);
        return new BillingPriceSnapshot(price.unitAmount(), price.currency());
    }

    private long toMinorUnits(BigDecimal amount) {
        return amount.multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();
    }

    private record BillingPriceSnapshot(BigDecimal unitAmount, String currency) {
    }
}
