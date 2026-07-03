package com.luccavergara.solaris.service;

import com.luccavergara.solaris.dto.*;
import com.luccavergara.solaris.entity.*;
import com.luccavergara.solaris.exception.DuplicateResourceException;
import com.luccavergara.solaris.exception.ResourceNotFoundException;
import com.luccavergara.solaris.repository.*;
import com.luccavergara.solaris.tenant.TenantContext;
import com.luccavergara.solaris.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrganizationInviteService {

    private static final int INVITE_EXPIRY_DAYS = 7;

    private final OrganizationInviteRepository organizationInviteRepository;
    private final OrganizationMemberRepository organizationMemberRepository;
    private final OrganizationRepository organizationRepository;
    private final StoreRepository storeRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final OrganizationMembershipService organizationMembershipService;
    private final AuthenticatedUserService authenticatedUserService;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Value("${application.frontend.url}")
    private String frontendUrl;

    public OrganizationInviteResponse createInvite(Long organizationId, OrganizationInviteRequest request) {
        validateOrganizationAccess(organizationId);
        validateInviteRole(request.getRole());

        String normalizedEmail = request.getEmail().trim().toLowerCase();

        if (organizationMemberRepository.existsByOrganizationIdAndUserEmailIgnoreCaseAndStatus(
                organizationId,
                normalizedEmail,
                OrganizationMemberStatus.ACTIVE
        )) {
            throw new DuplicateResourceException("User is already a member of this organization");
        }

        organizationInviteRepository
                .findByOrganizationIdAndEmailIgnoreCaseAndStatus(
                        organizationId,
                        normalizedEmail,
                        OrganizationInviteStatus.PENDING
                )
                .ifPresent(existing -> {
                    throw new DuplicateResourceException("A pending invite already exists for this email");
                });

        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));

        Store store = resolveStore(organizationId, request.getStoreId());
        User currentUser = authenticatedUserService.getCurrentUser();
        String token = UUID.randomUUID().toString();

        OrganizationInvite invite = OrganizationInvite.builder()
                .organization(organization)
                .email(normalizedEmail)
                .role(request.getRole())
                .store(store)
                .token(token)
                .expiresAt(LocalDateTime.now().plusDays(INVITE_EXPIRY_DAYS))
                .createdAt(LocalDateTime.now())
                .invitedBy(currentUser)
                .status(OrganizationInviteStatus.PENDING)
                .build();

        OrganizationInvite savedInvite = organizationInviteRepository.save(invite);

        String inviteLink = frontendUrl + "/accept-invite?token=" + token;
        emailService.sendOrganizationInvite(
                normalizedEmail,
                organization.getRazonSocial(),
                request.getRole().name(),
                inviteLink
        );

        return mapInviteToResponse(savedInvite);
    }

    public List<OrganizationMemberResponse> listMembers(Long organizationId) {
        validateOrganizationAccess(organizationId);

        List<OrganizationMemberResponse> members = new ArrayList<>();

        organizationMemberRepository.findAllByOrganizationId(organizationId).stream()
                .sorted(Comparator.comparing(member -> member.getUser().getEmail()))
                .forEach(member -> members.add(mapMemberToResponse(member)));

        organizationInviteRepository
                .findAllByOrganizationIdAndStatus(organizationId, OrganizationInviteStatus.PENDING)
                .stream()
                .sorted(Comparator.comparing(OrganizationInvite::getEmail))
                .forEach(invite -> members.add(mapInviteToMemberResponse(invite)));

        return members;
    }

    public List<StoreResponse> listStores(Long organizationId) {
        validateOrganizationAccess(organizationId);

        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));

        return storeRepository.findAllByOrganization(organization).stream()
                .map(store -> StoreResponse.builder()
                        .id(store.getId())
                        .name(store.getName())
                        .address(store.getAddress())
                        .afipPuntoVenta(store.getAfipPuntoVenta())
                        .active(store.getActive())
                        .build())
                .toList();
    }

    public void revokeInvite(Long organizationId, Long inviteId) {
        validateOrganizationAccess(organizationId);

        OrganizationInvite invite = organizationInviteRepository
                .findByIdAndOrganizationId(inviteId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Invite not found"));

        if (invite.getStatus() != OrganizationInviteStatus.PENDING) {
            throw new IllegalArgumentException("Only pending invites can be revoked");
        }

        invite.setStatus(OrganizationInviteStatus.REVOKED);
        organizationInviteRepository.save(invite);
    }

    public OrganizationInvitePreviewResponse previewInvite(String token) {
        OrganizationInvite invite = organizationInviteRepository.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Invite not found"));

        boolean expired = isExpired(invite);

        return OrganizationInvitePreviewResponse.builder()
                .email(invite.getEmail())
                .organizationName(invite.getOrganization().getRazonSocial())
                .role(invite.getRole())
                .existingUser(userRepository.findByEmail(invite.getEmail()).isPresent())
                .expired(expired)
                .build();
    }

    @Transactional
    public AuthenticationResponse acceptInvite(AcceptOrganizationInviteRequest request) {
        OrganizationInvite invite = organizationInviteRepository.findByToken(request.getToken())
                .orElseThrow(() -> new ResourceNotFoundException("Invite not found"));

        validatePendingInvite(invite);

        User existingUser = userRepository.findByEmail(invite.getEmail()).orElse(null);
        User resolvedUser;

        if (existingUser == null) {
            resolvedUser = registerInvitedUser(request, invite.getEmail());
        } else {
            resolvedUser = authenticateExistingUser(existingUser, request, invite.getEmail());
        }

        if (organizationMemberRepository.findByUserAndOrganizationId(resolvedUser, invite.getOrganization().getId())
                .filter(member -> member.getStatus() == OrganizationMemberStatus.ACTIVE)
                .isPresent()) {
            throw new DuplicateResourceException("User is already a member of this organization");
        }

        OrganizationMember membership = organizationMemberRepository
                .findByUserAndOrganizationId(resolvedUser, invite.getOrganization().getId())
                .orElseGet(() -> OrganizationMember.builder()
                        .user(resolvedUser)
                        .organization(invite.getOrganization())
                        .build());

        membership.setRole(invite.getRole());
        membership.setStore(invite.getStore());
        membership.setStatus(OrganizationMemberStatus.ACTIVE);
        organizationMemberRepository.save(membership);

        invite.setStatus(OrganizationInviteStatus.ACCEPTED);
        invite.setAcceptedAt(LocalDateTime.now());
        organizationInviteRepository.save(invite);

        String jwtToken = jwtService.generateToken(
                organizationMembershipService.buildJwtClaims(membership),
                resolvedUser
        );

        return AuthenticationResponse.builder()
                .token(jwtToken)
                .build();
    }

    private User registerInvitedUser(AcceptOrganizationInviteRequest request, String email) {
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new IllegalArgumentException("Password is required for new users");
        }

        if (request.getFirstname() == null || request.getFirstname().isBlank()
                || request.getLastname() == null || request.getLastname().isBlank()) {
            throw new IllegalArgumentException("First name and last name are required for new users");
        }

        User user = User.builder()
                .firstname(request.getFirstname().trim())
                .lastname(request.getLastname().trim())
                .email(email)
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .emailVerified(true)
                .build();

        User savedUser = userRepository.save(user);

        categoryRepository.save(
                Category.builder()
                        .name("General")
                        .description("Default category")
                        .createdAt(LocalDateTime.now())
                        .systemCategory(true)
                        .user(savedUser)
                        .build()
        );

        return savedUser;
    }

    private User authenticateExistingUser(
            User user,
            AcceptOrganizationInviteRequest request,
            String inviteEmail
    ) {
        Authentication currentAuth = SecurityContextHolder.getContext().getAuthentication();

        if (currentAuth != null
                && currentAuth.isAuthenticated()
                && currentAuth.getPrincipal() instanceof User authenticatedUser
                && authenticatedUser.getEmail().equalsIgnoreCase(inviteEmail)) {
            return authenticatedUser;
        }

        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new IllegalArgumentException("Password is required to accept this invite");
        }

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(inviteEmail, request.getPassword())
        );

        return userRepository.findByEmail(inviteEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private Store resolveStore(Long organizationId, Long storeId) {
        if (storeId == null) {
            return null;
        }

        organizationMembershipService.validateStoreInOrganization(organizationId, storeId);

        return storeRepository.findById(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Store not found"));
    }

    private void validateOrganizationAccess(Long organizationId) {
        Long currentOrgId = TenantContext.getOrganizationId();

        if (currentOrgId == null || !currentOrgId.equals(organizationId)) {
            throw new ResourceNotFoundException("Organization not found");
        }
    }

    private void validateInviteRole(OrganizationMemberRole role) {
        if (role == OrganizationMemberRole.OWNER) {
            throw new IllegalArgumentException("Cannot invite users with OWNER role");
        }
    }

    private void validatePendingInvite(OrganizationInvite invite) {
        if (invite.getStatus() != OrganizationInviteStatus.PENDING) {
            throw new IllegalArgumentException("Invite is no longer valid");
        }

        if (isExpired(invite)) {
            throw new IllegalArgumentException("Invite has expired");
        }
    }

    private boolean isExpired(OrganizationInvite invite) {
        return invite.getExpiresAt().isBefore(LocalDateTime.now());
    }

    private OrganizationInviteResponse mapInviteToResponse(OrganizationInvite invite) {
        return OrganizationInviteResponse.builder()
                .id(invite.getId())
                .email(invite.getEmail())
                .role(invite.getRole())
                .storeId(invite.getStore() != null ? invite.getStore().getId() : null)
                .storeName(invite.getStore() != null ? invite.getStore().getName() : null)
                .status(invite.getStatus().name())
                .expiresAt(invite.getExpiresAt())
                .createdAt(invite.getCreatedAt())
                .build();
    }

    private OrganizationMemberResponse mapMemberToResponse(OrganizationMember member) {
        return OrganizationMemberResponse.builder()
                .id(member.getId())
                .email(member.getUser().getEmail())
                .firstname(member.getUser().getFirstname())
                .lastname(member.getUser().getLastname())
                .role(member.getRole())
                .storeId(member.getStore() != null ? member.getStore().getId() : null)
                .storeName(member.getStore() != null ? member.getStore().getName() : null)
                .status(member.getStatus())
                .pendingInvite(false)
                .build();
    }

    private OrganizationMemberResponse mapInviteToMemberResponse(OrganizationInvite invite) {
        return OrganizationMemberResponse.builder()
                .id(invite.getId())
                .email(invite.getEmail())
                .role(invite.getRole())
                .storeId(invite.getStore() != null ? invite.getStore().getId() : null)
                .storeName(invite.getStore() != null ? invite.getStore().getName() : null)
                .status(OrganizationMemberStatus.INVITED)
                .createdAt(invite.getCreatedAt())
                .expiresAt(invite.getExpiresAt())
                .pendingInvite(true)
                .build();
    }
}
