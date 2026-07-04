package com.luccavergara.solaris.service;

import com.luccavergara.solaris.dto.BillingSessionTokenResponse;
import com.luccavergara.solaris.entity.OrganizationMemberRole;
import com.luccavergara.solaris.entity.OrganizationMemberStatus;
import com.luccavergara.solaris.entity.User;
import com.luccavergara.solaris.repository.OrganizationMemberRepository;
import com.luccavergara.solaris.security.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class BillingSessionService {

    private final JwtService jwtService;
    private final OrganizationMemberRepository organizationMemberRepository;

    @Value("${application.billing.session-token-expiration-ms:1800000}")
    private long billingSessionExpirationMs;

    public BillingSessionTokenResponse createSessionToken(Long organizationId, User user) {
        assertCanManageBilling(user, organizationId);

        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtService.CLAIM_ORG_ID, organizationId);
        claims.put(JwtService.CLAIM_USER_ID, user.getId());
        claims.put(JwtService.CLAIM_PURPOSE, JwtService.PURPOSE_ORG_BILLING);

        Instant expiresAt = Instant.now().plusMillis(billingSessionExpirationMs);
        String billingToken = jwtService.generateToken(claims, user, billingSessionExpirationMs);

        return BillingSessionTokenResponse.builder()
                .billingToken(billingToken)
                .expiresAt(LocalDateTime.ofInstant(expiresAt, ZoneOffset.UTC))
                .build();
    }

    public Long resolveOrganizationId(String billingToken) {
        return resolveSession(billingToken).organizationId();
    }

    public BillingSession resolveSession(String billingToken) {
        if (billingToken == null || billingToken.isBlank()) {
            throw new AccessDeniedException("Billing session token is required");
        }

        try {
            Claims claims = jwtService.parseClaims(billingToken);

            if (!JwtService.PURPOSE_ORG_BILLING.equals(claims.get(JwtService.CLAIM_PURPOSE, String.class))) {
                throw new AccessDeniedException("Invalid billing session token purpose");
            }

            Long organizationId = readLongClaim(claims, JwtService.CLAIM_ORG_ID);
            Long userId = readLongClaim(claims, JwtService.CLAIM_USER_ID);
            String email = claims.getSubject();

            if (organizationId == null || userId == null || email == null || email.isBlank()) {
                throw new AccessDeniedException("Billing session token is missing required claims");
            }

            organizationMemberRepository
                    .findRoleByOrganizationIdAndUserEmailIgnoreCaseAndStatus(
                            organizationId,
                            email,
                            OrganizationMemberStatus.ACTIVE
                    )
                    .filter(role -> role.getPrivilegeLevel() >= OrganizationMemberRole.ADMIN.getPrivilegeLevel())
                    .orElseThrow(() -> new AccessDeniedException(
                            "Billing session is no longer valid for organization " + organizationId
                    ));

            return new BillingSession(organizationId, userId, email);
        } catch (JwtException | IllegalArgumentException ex) {
            throw new AccessDeniedException("Billing session token is invalid or expired");
        }
    }

    private void assertCanManageBilling(User user, Long organizationId) {
        organizationMemberRepository
                .findRoleByOrganizationIdAndUserEmailIgnoreCaseAndStatus(
                        organizationId,
                        user.getEmail(),
                        OrganizationMemberStatus.ACTIVE
                )
                .filter(role -> role.getPrivilegeLevel() >= OrganizationMemberRole.ADMIN.getPrivilegeLevel())
                .orElseThrow(() -> new AccessDeniedException(
                        "You do not have billing permissions for organization " + organizationId
                ));
    }

    private Long readLongClaim(Claims claims, String claimName) {
        Object value = claims.get(claimName);

        if (value instanceof Number number) {
            return number.longValue();
        }

        if (value instanceof String string && !string.isBlank()) {
            return Long.parseLong(string.trim());
        }

        return null;
    }

    public record BillingSession(Long organizationId, Long userId, String email) {
    }
}
