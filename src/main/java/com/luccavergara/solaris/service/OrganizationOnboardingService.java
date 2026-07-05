package com.luccavergara.solaris.service;

import com.luccavergara.solaris.dto.AuthenticationResponse;
import com.luccavergara.solaris.dto.OnboardingStatusResponse;
import com.luccavergara.solaris.dto.SetupOrganizationRequest;
import com.luccavergara.solaris.entity.*;
import com.luccavergara.solaris.exception.DuplicateResourceException;
import com.luccavergara.solaris.repository.OrganizationMemberRepository;
import com.luccavergara.solaris.repository.OrganizationRepository;
import com.luccavergara.solaris.repository.StoreRepository;
import com.luccavergara.solaris.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrganizationOnboardingService {

    private static final String DEFAULT_STORE_NAME = "Sucursal principal";

    private final AuthenticatedUserService authenticatedUserService;
    private final OrganizationMemberRepository organizationMemberRepository;
    private final OrganizationRepository organizationRepository;
    private final StoreRepository storeRepository;
    private final OrganizationJurisdictionService organizationJurisdictionService;
    private final SubscriptionService subscriptionService;
    private final OrganizationMembershipService organizationMembershipService;
    private final JwtService jwtService;

    @Transactional(readOnly = true)
    public OnboardingStatusResponse getOnboardingStatus() {
        User user = authenticatedUserService.getCurrentUser();
        List<OrganizationMember> memberships = organizationMemberRepository.findAllByUserAndStatus(
                user,
                OrganizationMemberStatus.ACTIVE
        );

        if (memberships.isEmpty()) {
            return OnboardingStatusResponse.builder()
                    .emailVerified(Boolean.TRUE.equals(user.getEmailVerified()))
                    .needsOrganizationSetup(true)
                    .needsPlanSelection(false)
                    .build();
        }

        OrganizationMember membership = organizationMembershipService.findPrimaryMembership(user)
                .orElse(memberships.getFirst());

        Organization organization = membership.getOrganization();
        OrganizationSubscription subscription = subscriptionService.ensureSubscription(organization);

        boolean needsPlanSelection = subscription.getStatus() == SubscriptionStatus.PENDING_PLAN;
        String organizationName = resolveOrganizationName(organization);

        return OnboardingStatusResponse.builder()
                .emailVerified(Boolean.TRUE.equals(user.getEmailVerified()))
                .needsOrganizationSetup(false)
                .needsPlanSelection(needsPlanSelection)
                .organizationId(organization.getId())
                .organizationName(organizationName)
                .countryCode(organization.getCountryCode())
                .subscriptionStatus(subscription.getStatus())
                .build();
    }

    @Transactional
    public AuthenticationResponse setupOrganization(SetupOrganizationRequest request) {
        User user = authenticatedUserService.getCurrentUser();

        if (!Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new IllegalStateException("Verify your email before setting up your organization");
        }

        if (organizationMemberRepository.existsByUser(user)) {
            throw new DuplicateResourceException("You already belong to an organization");
        }

        String organizationName = request.getOrganizationName().trim();
        if (!StringUtils.hasText(organizationName)) {
            throw new IllegalArgumentException("Organization name is required");
        }

        String storeName = StringUtils.hasText(request.getStoreName())
                ? request.getStoreName().trim()
                : DEFAULT_STORE_NAME;

        Organization organization = Organization.builder()
                .razonSocial(organizationName)
                .displayName(organizationName)
                .condicionIva(defaultCondicionIva(request.getCountryCode()))
                .timezone(defaultTimezone(request.getCountryCode()))
                .build();

        organizationJurisdictionService.applyCountryDefaults(organization, request.getCountryCode());
        organization = organizationRepository.save(organization);

        Store store = storeRepository.save(
                Store.builder()
                        .organization(organization)
                        .name(storeName)
                        .active(true)
                        .build()
        );

        subscriptionService.createPendingPlanSubscription(organization);

        OrganizationMember membership = organizationMemberRepository.save(
                OrganizationMember.builder()
                        .user(user)
                        .organization(organization)
                        .role(OrganizationMemberRole.OWNER)
                        .store(store)
                        .status(OrganizationMemberStatus.ACTIVE)
                        .build()
        );

        String jwtToken = jwtService.generateToken(
                organizationMembershipService.buildJwtClaims(membership),
                user
        );

        return AuthenticationResponse.builder()
                .token(jwtToken)
                .build();
    }

    private CondicionIva defaultCondicionIva(CountryCode countryCode) {
        return countryCode == CountryCode.ES ? CondicionIva.RESPONSABLE_INSCRIPTO : CondicionIva.MONOTRIBUTO;
    }

    private String defaultTimezone(CountryCode countryCode) {
        return countryCode == CountryCode.ES
                ? "Europe/Madrid"
                : "America/Argentina/Buenos_Aires";
    }

    private String resolveOrganizationName(Organization organization) {
        if (StringUtils.hasText(organization.getDisplayName())) {
            return organization.getDisplayName().trim();
        }

        return organization.getRazonSocial();
    }
}
