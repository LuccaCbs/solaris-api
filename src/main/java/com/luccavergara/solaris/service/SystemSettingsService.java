package com.luccavergara.solaris.service;

import com.luccavergara.solaris.dto.SystemSettingsRequest;
import com.luccavergara.solaris.dto.SystemSettingsResponse;
import com.luccavergara.solaris.entity.SystemSettings;
import com.luccavergara.solaris.repository.SystemSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.luccavergara.solaris.dto.ValidateAdminPasswordRequest;
import com.luccavergara.solaris.dto.ValidateAdminPasswordResponse;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Service
@RequiredArgsConstructor
public class SystemSettingsService {

    private static final int DEFAULT_LOW_STOCK_THRESHOLD = 5;

    private final SystemSettingsRepository systemSettingsRepository;
    private final PasswordEncoder passwordEncoder;

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
            settings.setBusinessTimezone(request.getBusinessTimezone());
        }

        if (request.getCashRegisterAutoCloseTime() != null) {
            settings.setCashRegisterAutoCloseTime(request.getCashRegisterAutoCloseTime());
        }

        if (request.getWhatsappEnabled() != null) {
            settings.setWhatsappEnabled(request.getWhatsappEnabled());
        }

        return mapToResponse(systemSettingsRepository.save(settings));
    }

    public SystemSettings getOrCreateSettings() {
        return systemSettingsRepository.findAll()
                .stream()
                .findFirst()
                .orElseGet(() -> systemSettingsRepository.save(
                        SystemSettings.builder()
                                .globalLowStockThreshold(DEFAULT_LOW_STOCK_THRESHOLD)
                                .businessTimezone("America/Argentina/Buenos_Aires")
                                .cashRegisterAutoCloseTime(LocalTime.MIDNIGHT)
                                .updatedAt(LocalDateTime.now())
                                .whatsappEnabled(false)
                                .build()
                ));
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

        boolean valid = settings.getAdminAccessPasswordHash() != null
                && !settings.getAdminAccessPasswordHash().isBlank()
                && passwordEncoder.matches(
                request.getPassword(),
                settings.getAdminAccessPasswordHash()
        );

        return ValidateAdminPasswordResponse.builder()
                .valid(valid)
                .build();
    }
    public void validateAdminPasswordOrThrow(String password) {
        SystemSettings settings = getOrCreateSettings();

        boolean valid = settings.getAdminAccessPasswordHash() != null
                && !settings.getAdminAccessPasswordHash().isBlank()
                && passwordEncoder.matches(
                password,
                settings.getAdminAccessPasswordHash()
        );

        if (!valid) {
            throw new IllegalArgumentException("Invalid admin password");
        }
    }
}