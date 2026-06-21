package io.itsm.sla.calendar;

import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Праздничный день в производственном календаре.
 */
public record Holiday(
    LocalDate date,
    String name,
    HolidayType type,
    ZoneId zone
) {
    public Holiday {
        if (zone == null) zone = ZoneId.of("UTC");
    }

    /**
     * Является ли праздник нерабочим днём.
     */
    public boolean isNonWorking() {
        return type == HolidayType.PUBLIC
            || type == HolidayType.TRANSFERRED;
    }

    /**
     * Является ли день сокращённым.
     */
    public boolean isShortened() {
        return type == HolidayType.SHORTENED;
    }

    public enum HolidayType {
        PUBLIC,        // Государственный праздник — нерабочий день
        TRANSFERRED,   // Перенесённый день (рабочий/нерабочий)
        SHORTENED      // Сокращённый день (на час короче)
    }
}
