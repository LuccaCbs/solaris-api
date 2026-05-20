package com.luccavergara.solaris.service;

import com.luccavergara.solaris.dto.SystemSettingsRequest;
import com.luccavergara.solaris.dto.SystemSettingsResponse;
import com.luccavergara.solaris.entity.SystemSettings;
import com.luccavergara.solaris.repository.SystemSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SystemSettingsService {

    private static final int DEFAULT_LOW_STOCK_THRESHOLD = 5;

    private final SystemSettingsRepository systemSettingsRepository;

    public SystemSettingsResponse getSettings() {
        return mapToResponse(getOrCreateSettings());
    }

    public SystemSettingsResponse updateSettings(SystemSettingsRequest request) {
        SystemSettings settings = getOrCreateSettings();

        settings.setGlobalLowStockThreshold(request.getGlobalLowStockThreshold());
        settings.setUpdatedAt(LocalDateTime.now());

        return mapToResponse(systemSettingsRepository.save(settings));
    }

    public SystemSettings getOrCreateSettings() {
        return systemSettingsRepository.findAll()
                .stream()
                .findFirst()
                .orElseGet(() -> systemSettingsRepository.save(
                        SystemSettings.builder()
                                .globalLowStockThreshold(DEFAULT_LOW_STOCK_THRESHOLD)
                                .updatedAt(LocalDateTime.now())
                                .build()
                ));
    }

    private SystemSettingsResponse mapToResponse(SystemSettings settings) {
        return SystemSettingsResponse.builder()
                .id(settings.getId())
                .globalLowStockThreshold(settings.getGlobalLowStockThreshold())
                .updatedAt(settings.getUpdatedAt())
                .build();
    }
}