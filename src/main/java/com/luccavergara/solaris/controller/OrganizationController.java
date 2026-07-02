package com.luccavergara.solaris.controller;

import com.luccavergara.solaris.dto.*;
import com.luccavergara.solaris.service.FiscalDocumentService;
import com.luccavergara.solaris.service.OrganizationInviteService;
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
