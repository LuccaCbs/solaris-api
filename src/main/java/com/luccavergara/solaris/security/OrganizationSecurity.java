package com.luccavergara.solaris.security;

import com.luccavergara.solaris.entity.OrganizationMemberRole;
import com.luccavergara.solaris.entity.OrganizationMemberStatus;
import com.luccavergara.solaris.entity.User;
import com.luccavergara.solaris.entity.OrganizationMemberRole;
import com.luccavergara.solaris.repository.OrganizationMemberRepository;
import com.luccavergara.solaris.repository.UserRepository;
import com.luccavergara.solaris.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component("organizationSecurity")
@RequiredArgsConstructor
public class OrganizationSecurity {

    private static final ThreadLocal<String> LAST_DENIAL_REASON = new ThreadLocal<>();

    private final UserRepository userRepository;
    private final OrganizationMemberRepository organizationMemberRepository;

    public static String consumeLastDenialReason() {
        String reason = LAST_DENIAL_REASON.get();
        LAST_DENIAL_REASON.remove();
        return reason;
    }

    public boolean hasMinimumRole(OrganizationMemberRole minimumRole) {
        OrganizationMemberRole currentRole = resolveCurrentRole();

        if (currentRole == null) {
            recordDenial("JWT role missing for required minimum role " + minimumRole);
            return false;
        }

        boolean allowed = currentRole.getPrivilegeLevel() >= minimumRole.getPrivilegeLevel();

        if (!allowed) {
            recordDenial("JWT role " + currentRole + " is below required minimum " + minimumRole);
        }

        return allowed;
    }

    public boolean belongsToOrganization(Object organizationId) {
        Long orgId = toOrganizationId(organizationId);
        Long currentOrgId = TenantContext.getOrganizationId();

        if (orgId == null || currentOrgId == null) {
            return false;
        }

        return currentOrgId.longValue() == orgId.longValue();
    }

    public boolean canAccessOrganization(Object organizationId, OrganizationMemberRole minimumRole) {
        LAST_DENIAL_REASON.remove();

        Long orgId = toOrganizationId(organizationId);

        if (orgId == null) {
            recordDenial("Organization id could not be resolved from request context");
            log.warn("Organization access denied: unresolved organization id {}", organizationId);
            return false;
        }

        Optional<String> authenticatedEmail = resolveAuthenticatedEmail();

        if (authenticatedEmail.isEmpty()) {
            recordDenial("Request is not authenticated with a valid user session");
            log.warn("Organization access denied for orgId={}: unauthenticated request", orgId);
            return false;
        }

        Optional<OrganizationMemberRole> membershipRole = resolveMembershipRole(orgId, authenticatedEmail.get());

        if (membershipRole.isPresent()) {
            boolean allowed = membershipRole.get().getPrivilegeLevel() >= minimumRole.getPrivilegeLevel();

            if (!allowed) {
                recordDenial(
                        "User "
                                + authenticatedEmail.get()
                                + " has role "
                                + membershipRole.get()
                                + " in organization "
                                + orgId
                                + " but "
                                + minimumRole
                                + " is required"
                );
                log.warn(
                        "Organization access denied for orgId={}: role {} below minimum {}",
                        orgId,
                        membershipRole.get(),
                        minimumRole
                );
            }

            return allowed;
        }

        boolean allowed = belongsToOrganization(orgId) && hasMinimumRole(minimumRole);

        if (!allowed) {
            recordDenial(
                    "No active membership for "
                            + authenticatedEmail.get()
                            + " in organization "
                            + orgId
                            + ". JWT orgId="
                            + TenantContext.getOrganizationId()
                            + ", JWT role="
                            + TenantContext.getRole()
            );
            log.warn(
                    "Organization access denied for orgId={}: jwtOrgId={} jwtRole={}",
                    orgId,
                    TenantContext.getOrganizationId(),
                    TenantContext.getRole()
            );
        }

        return allowed;
    }

    public void requireOrganizationAccess(Long organizationId, OrganizationMemberRole minimumRole) {
        if (!canAccessOrganization(organizationId, minimumRole)) {
            String reason = consumeLastDenialReason();
            throw new AccessDeniedException(
                    reason != null && !reason.isBlank()
                            ? reason
                            : "You do not have permission to perform this action on organization "
                            + organizationId
            );
        }
    }

    public void requireBillingAccess(Long organizationId) {
        Long sessionOrgId = TenantContext.getOrganizationId();
        OrganizationMemberRole sessionRole = TenantContext.getRole();

        if (sessionOrgId == null) {
            throw new AccessDeniedException(
                    "No organization selected in your session. Sign out, sign in again, and select the organization."
            );
        }

        if (!sessionOrgId.equals(organizationId)) {
            throw new AccessDeniedException(
                    "Session organization "
                            + sessionOrgId
                            + " does not match requested organization "
                            + organizationId
                            + ". Select the correct organization and try again."
            );
        }

        if (sessionRole == null
                || sessionRole.getPrivilegeLevel() < OrganizationMemberRole.ADMIN.getPrivilegeLevel()) {
            throw new AccessDeniedException(
                    "Role "
                            + sessionRole
                            + " cannot manage billing for organization "
                            + organizationId
            );
        }
    }

    private Optional<OrganizationMemberRole> resolveMembershipRole(Long organizationId, String email) {
        Optional<OrganizationMemberRole> roleByEmail = organizationMemberRepository
                .findRoleByOrganizationIdAndUserEmailIgnoreCaseAndStatus(
                        organizationId,
                        email,
                        OrganizationMemberStatus.ACTIVE
                );

        if (roleByEmail.isPresent()) {
            return roleByEmail;
        }

        Optional<User> user = userRepository.findByEmailIgnoreCase(email);

        if (user.isEmpty()) {
            return Optional.empty();
        }

        return organizationMemberRepository
                .findByUserAndOrganizationIdAndStatus(
                        user.get(),
                        organizationId,
                        OrganizationMemberStatus.ACTIVE
                )
                .map(member -> member.getRole());
    }

    private Optional<String> resolveAuthenticatedEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof User user) {
            return Optional.ofNullable(user.getEmail());
        }

        if (principal instanceof UserDetails userDetails) {
            return Optional.ofNullable(userDetails.getUsername());
        }

        String email = authentication.getName();

        if (email == null || email.isBlank() || "anonymousUser".equals(email)) {
            return Optional.empty();
        }

        return Optional.of(email);
    }

    private OrganizationMemberRole resolveCurrentRole() {
        OrganizationMemberRole roleFromContext = TenantContext.getRole();
        if (roleFromContext != null) {
            return roleFromContext;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith("ORG_"))
                .findFirst()
                .map(authority -> OrganizationMemberRole.valueOf(authority.substring("ORG_".length())))
                .orElse(null);
    }

    private Long toOrganizationId(Object organizationId) {
        if (organizationId == null) {
            return null;
        }

        if (organizationId instanceof Number number) {
            return number.longValue();
        }

        if (organizationId instanceof String string && !string.isBlank()) {
            return Long.parseLong(string.trim());
        }

        return null;
    }

    private void recordDenial(String reason) {
        LAST_DENIAL_REASON.set(reason);
    }
}
