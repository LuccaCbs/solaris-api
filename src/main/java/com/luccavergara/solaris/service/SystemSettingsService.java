package com.luccavergara.solaris.service;

import com.luccavergara.solaris.dto.SystemSettingsRequest;
import com.luccavergara.solaris.dto.SystemSettingsResponse;
import com.luccavergara.solaris.dto.ValidateAdminPasswordRequest;
import com.luccavergara.solaris.dto.ValidateAdminPasswordResponse;
import com.luccavergara.solaris.entity.SystemSettings;
import com.luccavergara.solaris.entity.User;
import com.luccavergara.solaris.repository.SystemSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.luccavergara.solaris.entity.AuditAction;
import com.luccavergara.solaris.entity.AuditEntityType;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class SystemSettingsService {

    private static final int DEFAULT_LOW_STOCK_THRESHOLD = 5;

    private final SystemSettingsRepository systemSettingsRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticatedUserService authenticatedUserService;
    private final AuditLogService auditLogService;
    private final TenantQueryService tenantQueryService;
    private final TenantScopeService tenantScopeService;

    public SystemSettingsResponse getSettings() {
        return mapToResponse(getOrCreateSettings());
    }

    public SystemSettingsResponse updateSettings(SystemSettingsRequest request) {
        SystemSettings settings = getOrCreateSettings();

        settings.setGlobalLowStockThreshold(request.getGlobalLowStockThreshold());
        settings.setUpdatedAt(LocalDateTime.now());

        if (request.getAdminAccessPassword() != null
                && !request.getAdminAccessPassword().isBlank()) {
            settings.setAdminAccessPasswordHash(
                    passwordEncoder.encode(request.getAdminAccessPassword())
            );
        }

        if (request.getBusinessTimezone() != null && !request.getBusinessTimezone().isBlank()) {
            ZoneId.of(request.getBusinessTimezone());
            settings.setBusinessTimezone(request.getBusinessTimezone());
        }

        if (request.getCashRegisterAutoCloseTime() != null) {
            settings.setCashRegisterAutoCloseTime(request.getCashRegisterAutoCloseTime());
        }

        if (request.getWhatsappEnabled() != null) {
            settings.setWhatsappEnabled(request.getWhatsappEnabled());
        }

        SystemSettings savedSettings = systemSettingsRepository.save(settings);

        auditLogService.log(
                AuditAction.UPDATE_SETTINGS,
                AuditEntityType.SYSTEM_SETTINGS,
                savedSettings.getId(),
                "System Settings",
                "System settings updated"
        );

        return mapToResponse(savedSettings);
    }

    public SystemSettings getOrCreateSettings() {
        User currentUser = authenticatedUserService.getCurrentUser();

        return tenantQueryService.findSystemSettings()
                .orElseGet(() -> {
                    SystemSettings settings = SystemSettings.builder()
                            .globalLowStockThreshold(DEFAULT_LOW_STOCK_THRESHOLD)
                            .businessTimezone("America/Argentina/Buenos_Aires")
                            .cashRegisterAutoCloseTime(LocalTime.MIDNIGHT)
                            .updatedAt(LocalDateTime.now())
                            .whatsappEnabled(false)
                            .user(currentUser)
                            .build();
                    tenantScopeService.getOrganizationReference(currentUser)
                            .ifPresent(settings::setOrganization);
                    return systemSettingsRepository.save(settings);
                });
    }

    public boolean hasAdminAccessPasswordConfigured() {
        SystemSettings settings = getOrCreateSettings();

        return settings.getAdminAccessPasswordHash() != null
                && !settings.getAdminAccessPasswordHash().isBlank();
    }

    private SystemSettingsResponse mapToResponse(SystemSettings settings) {
        return SystemSettingsResponse.builder()
                .id(settings.getId())
                .globalLowStockThreshold(settings.getGlobalLowStockThreshold())
                .updatedAt(settings.getUpdatedAt())
                .businessTimezone(settings.getBusinessTimezone())
                .cashRegisterAutoCloseTime(settings.getCashRegisterAutoCloseTime())
                .hasAdminAccessPassword(
                        settings.getAdminAccessPasswordHash() != null
                                && !settings.getAdminAccessPasswordHash().isBlank()
                )
                .whatsappEnabled(settings.getWhatsappEnabled())
                .build();
    }

    public ValidateAdminPasswordResponse validateAdminPassword(
            ValidateAdminPasswordRequest request
    ) {
        SystemSettings settings = getOrCreateSettings();

        boolean hasPassword = settings.getAdminAccessPasswordHash() != null
                && !settings.getAdminAccessPasswordHash().isBlank();

        boolean valid = !hasPassword || passwordEncoder.matches(
                request.getPassword(),
                settings.getAdminAccessPasswordHash()
        );

        return ValidateAdminPasswordResponse.builder()
                .valid(valid)
                .build();
    }

    public void validateAdminPasswordOrThrow(String password) {
        SystemSettings settings = getOrCreateSettings();

        boolean hasPassword = settings.getAdminAccessPasswordHash() != null
                && !settings.getAdminAccessPasswordHash().isBlank();

        if (!hasPassword) {
            return;
        }

        boolean valid = password != null
                && passwordEncoder.matches(password, settings.getAdminAccessPasswordHash());

        if (!valid) {
            throw new IllegalArgumentException("Invalid admin password");
        }
    }

    public List<SystemSettings> getAllSettings() {
        return systemSettingsRepository.findAll();
    }
}