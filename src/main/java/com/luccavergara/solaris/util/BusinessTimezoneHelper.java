package com.luccavergara.solaris.util;

import com.luccavergara.solaris.service.SystemSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

@Component
@RequiredArgsConstructor
public class BusinessTimezoneHelper {

    private static final ZoneId STORAGE_ZONE = ZoneId.of("UTC");

    private final SystemSettingsService systemSettingsService;

    public ZoneId getBusinessZoneId() {
        return ZoneId.of(systemSettingsService.getOrCreateSettings().getBusinessTimezone());
    }

    public LocalDate today() {
        return LocalDate.now(getBusinessZoneId());
    }

    public LocalDateTime nowForStorage() {
        return LocalDateTime.now(STORAGE_ZONE);
    }

    public LocalDate toBusinessDate(LocalDateTime storedDateTime) {
        return storedDateTime
                .atZone(STORAGE_ZONE)
                .withZoneSameInstant(getBusinessZoneId())
                .toLocalDate();
    }

    public LocalDateTime businessDayStartForQuery(LocalDate businessDate) {
        return businessDate
                .atStartOfDay(getBusinessZoneId())
                .withZoneSameInstant(STORAGE_ZONE)
                .toLocalDateTime();
    }

    public LocalDateTime businessDayEndForQuery(LocalDate businessDate) {
        return businessDate
                .atTime(LocalTime.MAX)
                .atZone(getBusinessZoneId())
                .withZoneSameInstant(STORAGE_ZONE)
                .toLocalDateTime();
    }
}
