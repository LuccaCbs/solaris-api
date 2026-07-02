package com.luccavergara.solaris.controller;

import com.luccavergara.solaris.dto.SystemSettingsRequest;
import com.luccavergara.solaris.dto.SystemSettingsResponse;
import com.luccavergara.solaris.service.SystemSettingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.luccavergara.solaris.dto.ValidateAdminPasswordRequest;
import com.luccavergara.solaris.dto.ValidateAdminPasswordResponse;

@RestController
@RequestMapping("/api/v1/admin/settings")
@RequiredArgsConstructor
@PreAuthorize("@organizationSecurity.hasMinimumRole(T(com.luccavergara.solaris.entity.OrganizationMemberRole).ADMIN)")
public class SystemSettingsController {

    private final SystemSettingsService systemSettingsService;

    @GetMapping
    public ResponseEntity<SystemSettingsResponse> getSettings() {
        return ResponseEntity.ok(systemSettingsService.getSettings());
    }

    @PutMapping
    public ResponseEntity<SystemSettingsResponse> updateSettings(
            @Valid @RequestBody SystemSettingsRequest request
    ) {
        return ResponseEntity.ok(systemSettingsService.updateSettings(request));
    }

    @PostMapping("/validate-password")
    public ValidateAdminPasswordResponse validateAdminPassword(
            @Valid @RequestBody ValidateAdminPasswordRequest request
    ) {
        return systemSettingsService.validateAdminPassword(request);
    }
}