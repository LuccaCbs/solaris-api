package com.luccavergara.solaris.controller;

import com.luccavergara.solaris.dto.*;
import com.luccavergara.solaris.service.FiscalDocumentService;
import com.luccavergara.solaris.service.OrganizationInviteService;
import com.luccavergara.solaris.service.OrganizationService;
import com.luccavergara.solaris.service.StoreService;
import com.luccavergara.solaris.service.SubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/organizations")
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationInviteService organizationInviteService;
    private final FiscalDocumentService fiscalDocumentService;
    private final SubscriptionService subscriptionService;
    private final StoreService storeService;
    private final OrganizationService organizationService;

    @GetMapping("/{orgId}")
    @PreAuthorize("@organizationSecurity.hasMinimumRole(T(com.luccavergara.solaris.entity.OrganizationMemberRole).ADMIN)")
    public OrganizationResponse getOrganization(@PathVariable Long orgId) {
        return organizationService.getOrganization(orgId);
    }

    @PatchMapping("/{orgId}")
    @PreAuthorize("@organizationSecurity.hasMinimumRole(T(com.luccavergara.solaris.entity.OrganizationMemberRole).ADMIN)")
    public OrganizationResponse updateOrganization(
            @PathVariable Long orgId,
            @Valid @RequestBody UpdateOrganizationRequest request
    ) {
        return organizationService.updateOrganization(orgId, request);
    }

    @PatchMapping("/{orgId}/members/{memberId}")
    @PreAuthorize("@organizationSecurity.hasMinimumRole(T(com.luccavergara.solaris.entity.OrganizationMemberRole).ADMIN)")
    public void updateMemberRole(
            @PathVariable Long orgId,
            @PathVariable Long memberId,
            @Valid @RequestBody UpdateOrganizationMemberRoleRequest request
    ) {
        organizationService.updateMemberRole(orgId, memberId, request);
    }

    @PatchMapping("/{orgId}/stores/{storeId}")
    @PreAuthorize("@organizationSecurity.hasMinimumRole(T(com.luccavergara.solaris.entity.OrganizationMemberRole).ADMIN)")
    public StoreResponse updateStore(
            @PathVariable Long orgId,
            @PathVariable Long storeId,
            @Valid @RequestBody UpdateStoreRequest request
    ) {
        return storeService.updateStore(orgId, storeId, request);
    }

    @PostMapping("/{orgId}/invites")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@organizationSecurity.hasMinimumRole(T(com.luccavergara.solaris.entity.OrganizationMemberRole).ADMIN)")
    public OrganizationInviteResponse createInvite(
            @PathVariable Long orgId,
            @Valid @RequestBody OrganizationInviteRequest request
    ) {
        return organizationInviteService.createInvite(orgId, request);
    }

    @GetMapping("/{orgId}/members")
    @PreAuthorize("@organizationSecurity.hasMinimumRole(T(com.luccavergara.solaris.entity.OrganizationMemberRole).ADMIN)")
    public List<OrganizationMemberResponse> listMembers(@PathVariable Long orgId) {
        return organizationInviteService.listMembers(orgId);
    }

    @GetMapping("/{orgId}/stores")
    @PreAuthorize("@organizationSecurity.hasMinimumRole(T(com.luccavergara.solaris.entity.OrganizationMemberRole).ADMIN)")
    public List<StoreResponse> listStores(@PathVariable Long orgId) {
        return organizationInviteService.listStores(orgId);
    }

    @PostMapping("/{orgId}/stores")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@organizationSecurity.hasMinimumRole(T(com.luccavergara.solaris.entity.OrganizationMemberRole).ADMIN)")
    public StoreResponse createStore(
            @PathVariable Long orgId,
            @Valid @RequestBody CreateStoreRequest request
    ) {
        return storeService.createStore(orgId, request);
    }

    @GetMapping("/{orgId}/subscription")
    @PreAuthorize("@organizationSecurity.canAccessOrganization(#orgId, T(com.luccavergara.solaris.entity.OrganizationMemberRole).ADMIN)")
    public OrganizationSubscriptionResponse getSubscription(@PathVariable Long orgId) {
        return subscriptionService.getSubscription(orgId);
    }

    @PostMapping("/{orgId}/subscription/store-addon/checkout")
    @PreAuthorize("@organizationSecurity.canAccessOrganization(#orgId, T(com.luccavergara.solaris.entity.OrganizationMemberRole).ADMIN)")
    public StoreAddonCheckoutResponse initiateStoreAddonCheckout(
            @PathVariable Long orgId,
            @Valid @RequestBody StoreAddonCheckoutRequest request
    ) {
        return subscriptionService.initiateStoreAddonCheckout(orgId, request);
    }

    @PostMapping("/{orgId}/subscription/store-addon/mock-purchase")
    @PreAuthorize("@organizationSecurity.canAccessOrganization(#orgId, T(com.luccavergara.solaris.entity.OrganizationMemberRole).ADMIN)")
    public OrganizationSubscriptionResponse purchaseStoreAddonMock(
            @PathVariable Long orgId,
            @Valid @RequestBody StoreAddonCheckoutRequest request
    ) {
        return subscriptionService.purchaseStoreAddonMock(orgId, request);
    }

    @DeleteMapping("/{orgId}/invites/{inviteId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@organizationSecurity.hasMinimumRole(T(com.luccavergara.solaris.entity.OrganizationMemberRole).ADMIN)")
    public void revokeInvite(
            @PathVariable Long orgId,
            @PathVariable Long inviteId
    ) {
        organizationInviteService.revokeInvite(orgId, inviteId);
    }

    @GetMapping("/invites/preview")
    public OrganizationInvitePreviewResponse previewInvite(@RequestParam String token) {
        return organizationInviteService.previewInvite(token);
    }

    @PostMapping("/invites/accept")
    public AuthenticationResponse acceptInvite(
            @Valid @RequestBody AcceptOrganizationInviteRequest request
    ) {
        return organizationInviteService.acceptInvite(request);
    }

    @GetMapping("/{orgId}/fiscal-config")
    @PreAuthorize("@organizationSecurity.hasMinimumRole(T(com.luccavergara.solaris.entity.OrganizationMemberRole).ADMIN)")
    public FiscalConfigResponse getFiscalConfig(@PathVariable Long orgId) {
        return fiscalDocumentService.getFiscalConfig(orgId);
    }

    @PutMapping("/{orgId}/fiscal-config")
    @PreAuthorize("@organizationSecurity.hasMinimumRole(T(com.luccavergara.solaris.entity.OrganizationMemberRole).ADMIN)")
    public FiscalConfigResponse updateFiscalConfig(
            @PathVariable Long orgId,
            @Valid @RequestBody FiscalConfigRequest request
    ) {
        return fiscalDocumentService.updateFiscalConfig(orgId, request);
    }
}
