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
                    Long storeId = jwtService.extractStoreId(jwt).orElse(null);

                    User user = userRepository.findByEmail(userEmail).orElse(null);

                    if (user != null && organizationId != null) {
                        OrganizationMember membership = organizationMemberRepository
                                .findByUserAndOrganizationIdAndStatus(
                                        user,
                                        organizationId,
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

                    if (organizationId != null) {
                        TenantContext.setOrganizationId(organizationId);
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

                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    new ArrayList<>(authorities)
                            );

                    authToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );

                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (JwtException | IllegalArgumentException ignored) {
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
}
