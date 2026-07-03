package com.luccavergara.solaris.service;

import com.luccavergara.solaris.dto.*;
import com.luccavergara.solaris.entity.*;
import com.luccavergara.solaris.exception.DuplicateResourceException;
import com.luccavergara.solaris.exception.ResourceNotFoundException;
import com.luccavergara.solaris.repository.OrganizationRepository;
import com.luccavergara.solaris.repository.OrganizationSubscriptionRepository;
import com.luccavergara.solaris.repository.PromoCodeRedemptionRepository;
import com.luccavergara.solaris.repository.PromoCodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PromoCodeService {

    private final PromoCodeRepository promoCodeRepository;
    private final PromoCodeRedemptionRepository promoCodeRedemptionRepository;
    private final OrganizationRepository organizationRepository;
    private final OrganizationSubscriptionRepository subscriptionRepository;
    private final OrganizationMembershipService organizationMembershipService;
    private final AuthenticatedUserService authenticatedUserService;
    private final EntitlementService entitlementService;

    @Transactional(readOnly = true)
    public List<PromoCodeResponse> listPromoCodes() {
        return promoCodeRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::mapPromoCode)
                .toList();
    }

    @Transactional(readOnly = true)
    public PromoCodeResponse getPromoCode(Long promoCodeId) {
        PromoCode promoCode = promoCodeRepository.findById(promoCodeId)
                .orElseThrow(() -> new ResourceNotFoundException("Promo code not found"));

        return mapPromoCode(promoCode);
    }

    @Transactional(readOnly = true)
    public List<PromoCodeRedemptionResponse> listPromoCodeRedemptions(Long promoCodeId) {
        if (!promoCodeRepository.existsById(promoCodeId)) {
            throw new ResourceNotFoundException("Promo code not found");
        }

        return promoCodeRedemptionRepository.findAllByPromoCodeIdOrderByCreatedAtDesc(promoCodeId)
                .stream()
                .map(this::mapRedemption)
                .toList();
    }

    @Transactional
    public PromoCodeResponse createPromoCode(CreatePromoCodeRequest request) {
        User currentUser = authenticatedUserService.getCurrentUser();
        validateCreateRequest(request);

        String normalizedCode = normalizeCode(request.getCode());

        if (promoCodeRepository.existsByCodeNormalized(normalizedCode)) {
            throw new DuplicateResourceException("A promo code with this value already exists");
        }

        LocalDateTime now = LocalDateTime.now();

        PromoCode promoCode = PromoCode.builder()
                .code(request.getCode().trim())
                .codeNormalized(normalizedCode)
                .promoType(request.getPromoType())
                .grantPlanCode(request.getGrantPlanCode())
                .grantModuleCode(request.getGrantModuleCode())
                .durationDays(request.getDurationDays())
                .maxRedemptions(request.getMaxRedemptions())
                .status(PromoCodeStatus.ACTIVE)
                .validFrom(request.getValidFrom() != null ? request.getValidFrom() : now)
                .validUntil(request.getValidUntil())
                .internalNote(trimToNull(request.getInternalNote()))
                .createdByUserId(currentUser.getId())
                .redemptionCount(0)
                .createdAt(now)
                .updatedAt(now)
                .build();

        return mapPromoCode(promoCodeRepository.save(promoCode));
    }

    @Transactional
    public PromoCodeResponse revokePromoCode(Long promoCodeId, RevokePromoCodeRequest request) {
        User currentUser = authenticatedUserService.getCurrentUser();
        PromoCode promoCode = promoCodeRepository.findById(promoCodeId)
                .orElseThrow(() -> new ResourceNotFoundException("Promo code not found"));

        if (promoCode.getStatus() == PromoCodeStatus.REVOKED) {
            return mapPromoCode(promoCode);
        }

        LocalDateTime now = LocalDateTime.now();
        String reason = trimToNull(request != null ? request.getReason() : null);

        promoCode.setStatus(PromoCodeStatus.REVOKED);
        promoCode.setRevokedAt(now);
        promoCode.setRevokedByUserId(currentUser.getId());
        promoCode.setRevokeReason(reason);
        promoCode.setUpdatedAt(now);

        List<PromoCodeRedemption> activeRedemptions = promoCodeRedemptionRepository
                .findAllByPromoCodeIdAndStatus(promoCodeId, PromoRedemptionStatus.ACTIVE);

        for (PromoCodeRedemption redemption : activeRedemptions) {
            redemption.setStatus(PromoRedemptionStatus.REVOKED);
            redemption.setRevokedAt(now);
        }

        promoCodeRedemptionRepository.saveAll(activeRedemptions);

        return mapPromoCode(promoCodeRepository.save(promoCode));
    }

    @Transactional
    public PromoCodeResponse disablePromoCode(Long promoCodeId) {
        PromoCode promoCode = promoCodeRepository.findById(promoCodeId)
                .orElseThrow(() -> new ResourceNotFoundException("Promo code not found"));

        if (promoCode.getStatus() == PromoCodeStatus.REVOKED) {
            throw new IllegalStateException("Revoked promo codes cannot be disabled");
        }

        promoCode.setStatus(PromoCodeStatus.DISABLED);
        promoCode.setUpdatedAt(LocalDateTime.now());

        return mapPromoCode(promoCodeRepository.save(promoCode));
    }

    @Transactional
    public RedeemPromoCodeResponse redeemPromoCode(Long organizationId, RedeemPromoCodeRequest request) {
        User currentUser = authenticatedUserService.getCurrentUser();
        assertCanManageOrganizationPromoRedemption(organizationId, currentUser);

        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));

        String normalizedCode = normalizeCode(request.getCode());
        PromoCode promoCode = promoCodeRepository.findByCodeNormalized(normalizedCode)
                .orElseThrow(() -> new ResourceNotFoundException("Promo code not found"));

        LocalDateTime now = LocalDateTime.now();
        persistDerivedStatusIfNeeded(promoCode, now);

        if (!isRedeemable(promoCode, now)) {
            throw new IllegalStateException("This promo code is not available for redemption");
        }

        if (promoCodeRedemptionRepository.existsByPromoCodeIdAndOrganizationId(
                promoCode.getId(),
                organizationId
        )) {
            throw new DuplicateResourceException("This organization has already redeemed this promo code");
        }

        LocalDateTime accessValidUntil = resolveAccessValidUntil(promoCode, now);
        PromoCodeRedemption redemption = buildRedemption(
                promoCode,
                organization,
                currentUser.getId(),
                now,
                accessValidUntil
        );

        applyRedemptionSideEffects(promoCode, organization, now, accessValidUntil);

        promoCode.setRedemptionCount(promoCode.getRedemptionCount() + 1);
        persistDerivedStatusIfNeeded(promoCode, now);
        promoCode.setUpdatedAt(now);

        PromoCodeRedemption savedRedemption = promoCodeRedemptionRepository.save(redemption);
        promoCodeRepository.save(promoCode);

        return RedeemPromoCodeResponse.builder()
                .message("Promo code redeemed successfully")
                .redemption(mapRedemption(savedRedemption))
                .entitlements(entitlementService.getEntitlements(organizationId))
                .build();
    }

    private PromoCodeRedemption buildRedemption(
            PromoCode promoCode,
            Organization organization,
            Long redeemedByUserId,
            LocalDateTime now,
            LocalDateTime accessValidUntil
    ) {
        PromoCodeRedemption.PromoCodeRedemptionBuilder builder = PromoCodeRedemption.builder()
                .promoCodeId(promoCode.getId())
                .organization(organization)
                .redeemedByUserId(redeemedByUserId)
                .status(PromoRedemptionStatus.ACTIVE)
                .accessValidFrom(now)
                .accessValidUntil(accessValidUntil)
                .createdAt(now);

        if (promoCode.getPromoType() == PromoCodeType.GRANT_PLAN) {
            builder.grantedPlanCode(promoCode.getGrantPlanCode());
        } else if (promoCode.getPromoType() == PromoCodeType.GRANT_MODULE) {
            builder.grantedModuleCode(promoCode.getGrantModuleCode());
        }

        return builder.build();
    }

    private void applyRedemptionSideEffects(
            PromoCode promoCode,
            Organization organization,
            LocalDateTime now,
            LocalDateTime accessValidUntil
    ) {
        if (promoCode.getPromoType() != PromoCodeType.EXTEND_ACCESS) {
            return;
        }

        OrganizationSubscription subscription = subscriptionRepository.findByOrganization(organization)
                .orElseThrow(() -> new IllegalStateException("Organization subscription not found"));

        LocalDateTime extensionBase = subscription.getCurrentPeriodEnd();

        if (extensionBase == null || extensionBase.isBefore(now)) {
            extensionBase = now;
        }

        LocalDateTime extendedUntil = accessValidUntil != null
                ? accessValidUntil
                : extensionBase.plusDays(promoCode.getDurationDays() != null ? promoCode.getDurationDays() : 30);

        subscription.setCurrentPeriodEnd(extendedUntil);

        if (subscription.getStatus() == SubscriptionStatus.TRIALING) {
            LocalDateTime trialBase = subscription.getTrialEndsAt();

            if (trialBase == null || trialBase.isBefore(now)) {
                trialBase = now;
            }

            subscription.setTrialEndsAt(
                    accessValidUntil != null
                            ? accessValidUntil
                            : trialBase.plusDays(promoCode.getDurationDays() != null ? promoCode.getDurationDays() : 30)
            );
        }

        if (!subscription.isBillingActive()) {
            subscription.setStatus(SubscriptionStatus.ACTIVE);
        }

        subscription.setUpdatedAt(now);
        subscriptionRepository.save(subscription);
    }

    private LocalDateTime resolveAccessValidUntil(PromoCode promoCode, LocalDateTime now) {
        if (promoCode.getDurationDays() == null || promoCode.getDurationDays() <= 0) {
            return null;
        }

        return now.plusDays(promoCode.getDurationDays());
    }

    private void validateCreateRequest(CreatePromoCodeRequest request) {
        if (request.getPromoType() == PromoCodeType.GRANT_PLAN && request.getGrantPlanCode() == null) {
            throw new IllegalArgumentException("grantPlanCode is required for GRANT_PLAN promo codes");
        }

        if (request.getPromoType() == PromoCodeType.GRANT_MODULE && request.getGrantModuleCode() == null) {
            throw new IllegalArgumentException("grantModuleCode is required for GRANT_MODULE promo codes");
        }

        if (request.getPromoType() == PromoCodeType.EXTEND_ACCESS && request.getDurationDays() == null) {
            throw new IllegalArgumentException("durationDays is required for EXTEND_ACCESS promo codes");
        }

        if (request.getValidFrom() != null
                && request.getValidUntil() != null
                && !request.getValidUntil().isAfter(request.getValidFrom())) {
            throw new IllegalArgumentException("validUntil must be after validFrom");
        }
    }

    private void assertCanManageOrganizationPromoRedemption(Long organizationId, User user) {
        OrganizationMember membership = organizationMembershipService.resolveMembershipForOrganization(
                user,
                organizationId
        );

        if (membership.getRole().getPrivilegeLevel() < OrganizationMemberRole.ADMIN.getPrivilegeLevel()) {
            throw new AccessDeniedException("Insufficient permissions to redeem promo codes for this organization");
        }
    }

    private PromoCodeStatus resolveEffectiveStatus(PromoCode promoCode, LocalDateTime now) {
        if (promoCode.getStatus() == PromoCodeStatus.REVOKED
                || promoCode.getStatus() == PromoCodeStatus.DISABLED) {
            return promoCode.getStatus();
        }

        if (promoCode.getValidUntil() != null && !now.isBefore(promoCode.getValidUntil())) {
            return PromoCodeStatus.EXPIRED;
        }

        if (promoCode.getMaxRedemptions() != null
                && promoCode.getRedemptionCount() >= promoCode.getMaxRedemptions()) {
            return PromoCodeStatus.EXHAUSTED;
        }

        return promoCode.getStatus();
    }

    private boolean isRedeemable(PromoCode promoCode, LocalDateTime now) {
        PromoCodeStatus effectiveStatus = resolveEffectiveStatus(promoCode, now);

        if (effectiveStatus != PromoCodeStatus.ACTIVE) {
            return false;
        }

        if (promoCode.getValidFrom() != null && now.isBefore(promoCode.getValidFrom())) {
            return false;
        }

        if (promoCode.getMaxRedemptions() != null && promoCode.getRedemptionCount() >= promoCode.getMaxRedemptions()) {
            return false;
        }

        return true;
    }

    private void persistDerivedStatusIfNeeded(PromoCode promoCode, LocalDateTime now) {
        PromoCodeStatus effectiveStatus = resolveEffectiveStatus(promoCode, now);

        if (effectiveStatus != promoCode.getStatus()) {
            promoCode.setStatus(effectiveStatus);
        }
    }

    private PromoCodeResponse mapPromoCode(PromoCode promoCode) {
        LocalDateTime now = LocalDateTime.now();
        PromoCodeStatus effectiveStatus = resolveEffectiveStatus(promoCode, now);

        return PromoCodeResponse.builder()
                .id(promoCode.getId())
                .code(promoCode.getCode())
                .promoType(promoCode.getPromoType())
                .grantPlanCode(promoCode.getGrantPlanCode())
                .grantModuleCode(promoCode.getGrantModuleCode())
                .durationDays(promoCode.getDurationDays())
                .maxRedemptions(promoCode.getMaxRedemptions())
                .redemptionCount(promoCode.getRedemptionCount())
                .status(effectiveStatus)
                .validFrom(promoCode.getValidFrom())
                .validUntil(promoCode.getValidUntil())
                .internalNote(promoCode.getInternalNote())
                .createdByUserId(promoCode.getCreatedByUserId())
                .revokedAt(promoCode.getRevokedAt())
                .revokeReason(promoCode.getRevokeReason())
                .createdAt(promoCode.getCreatedAt())
                .updatedAt(promoCode.getUpdatedAt())
                .redeemable(isRedeemable(promoCode, now))
                .build();
    }

    private PromoCodeRedemptionResponse mapRedemption(PromoCodeRedemption redemption) {
        String promoCodeValue = promoCodeRepository.findById(redemption.getPromoCodeId())
                .map(PromoCode::getCode)
                .orElse(null);

        return PromoCodeRedemptionResponse.builder()
                .id(redemption.getId())
                .promoCodeId(redemption.getPromoCodeId())
                .promoCode(promoCodeValue)
                .organizationId(redemption.getOrganization().getId())
                .organizationName(redemption.getOrganization().getDisplayName())
                .redeemedByUserId(redemption.getRedeemedByUserId())
                .status(redemption.getStatus())
                .grantedPlanCode(redemption.getGrantedPlanCode())
                .grantedModuleCode(redemption.getGrantedModuleCode())
                .accessValidFrom(redemption.getAccessValidFrom())
                .accessValidUntil(redemption.getAccessValidUntil())
                .createdAt(redemption.getCreatedAt())
                .revokedAt(redemption.getRevokedAt())
                .build();
    }

    public static String normalizeCode(String code) {
        if (!StringUtils.hasText(code)) {
            throw new IllegalArgumentException("Promo code is required");
        }

        return code.trim().toUpperCase().replaceAll("\\s+", "");
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        return value.trim();
    }
}
