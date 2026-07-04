package com.luccavergara.solaris.security;

import com.luccavergara.solaris.entity.OrganizationMember;
import com.luccavergara.solaris.entity.OrganizationMemberRole;
import com.luccavergara.solaris.entity.OrganizationMemberStatus;
import com.luccavergara.solaris.entity.User;
import com.luccavergara.solaris.repository.OrganizationMemberRepository;
import com.luccavergara.solaris.repository.UserRepository;
import com.luccavergara.solaris.tenant.TenantContext;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final UserRepository userRepository;
    private final OrganizationMemberRepository organizationMemberRepository;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            try {
                filterChain.doFilter(request, response);
            } finally {
                TenantContext.clear();
            }
            return;
        }

        final String jwt = authHeader.substring(7).trim();

        if (jwt.isEmpty() || "null".equalsIgnoreCase(jwt) || "undefined".equalsIgnoreCase(jwt)) {
            try {
                filterChain.doFilter(request, response);
            } finally {
                TenantContext.clear();
            }
            return;
        }

        try {
            final String userEmail = jwtService.extractUsername(jwt);

            if (userEmail != null) {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

                if (jwtService.isTokenValid(jwt, userDetails)) {
                    OrganizationMemberRole organizationRole = jwtService.extractOrganizationRole(jwt).orElse(null);
                    Long organizationId = jwtService.extractOrganizationId(jwt).orElse(null);
                    Long pathOrganizationId = extractOrganizationIdFromPath(request);
                    Long roleResolutionOrgId = pathOrganizationId != null ? pathOrganizationId : organizationId;
                    Long storeId = jwtService.extractStoreId(jwt).orElse(null);

                    User user = userRepository.findByEmailIgnoreCase(userEmail).orElse(null);

                    if (user != null && roleResolutionOrgId != null) {
                        OrganizationMember membership = organizationMemberRepository
                                .findByUserAndOrganizationIdAndStatus(
                                        user,
                                        roleResolutionOrgId,
                                        OrganizationMemberStatus.ACTIVE
                                )
                                .orElse(null);

                        if (membership != null) {
                            organizationRole = membership.getRole();

                            if (membership.getStore() != null) {
                                storeId = membership.getStore().getId();
                            }
                        }
                    }

                    Long tenantOrganizationId = pathOrganizationId != null ? pathOrganizationId : organizationId;

                    if (tenantOrganizationId != null) {
                        TenantContext.setOrganizationId(tenantOrganizationId);
                    }

                    if (organizationRole != null) {
                        TenantContext.setRole(organizationRole);
                    }

                    if (storeId != null) {
                        TenantContext.setStoreId(storeId);
                    }

                    Set<SimpleGrantedAuthority> authorities = new LinkedHashSet<>(
                            userDetails.getAuthorities()
                                    .stream()
                                    .map(authority -> new SimpleGrantedAuthority(authority.getAuthority()))
                                    .toList()
                    );

                    if (organizationRole != null) {
                        authorities.add(new SimpleGrantedAuthority(toOrgAuthority(organizationRole)));
                    }

                    Object principal = user != null ? user : userDetails;

                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    principal,
                                    null,
                                    new ArrayList<>(authorities)
                            );

                    authToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );

                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (JwtException | IllegalArgumentException ex) {
            log.warn("JWT authentication failed for {} {}: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());
            SecurityContextHolder.clearContext();
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();

        if ("/api/v1/auth/select-organization".equals(path)) {
            return false;
        }

        return path.startsWith("/api/v1/auth")
                || path.equals("/api/v1/organizations/invites/accept")
                || path.equals("/api/v1/organizations/invites/preview")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/swagger-ui");
    }

    private String toOrgAuthority(OrganizationMemberRole role) {
        return "ORG_" + role.name();
    }

    private Long extractOrganizationIdFromPath(HttpServletRequest request) {
        Long fromServletPath = parseOrganizationIdFromPath(request.getServletPath());
        if (fromServletPath != null) {
            return fromServletPath;
        }

        String requestUri = request.getRequestURI();
        String contextPath = request.getContextPath();

        if (contextPath != null && !contextPath.isBlank() && requestUri.startsWith(contextPath)) {
            requestUri = requestUri.substring(contextPath.length());
        }

        return parseOrganizationIdFromPath(requestUri);
    }

    private Long parseOrganizationIdFromPath(String path) {
        if (path == null || !path.startsWith("/api/v1/organizations/")) {
            return null;
        }

        String remainder = path.substring("/api/v1/organizations/".length());

        if (remainder.isBlank() || remainder.startsWith("invites")) {
            return null;
        }

        int slashIndex = remainder.indexOf('/');
        String organizationIdPart = slashIndex >= 0 ? remainder.substring(0, slashIndex) : remainder;

        try {
            return Long.parseLong(organizationIdPart);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
