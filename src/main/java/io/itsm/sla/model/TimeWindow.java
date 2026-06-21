package io.itsm.sla.model;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.MonthDay;
import java.time.ZoneId;
import java.util.Set;

/**
 * Расписание рабочего времени — определяет, когда время считается рабочим.
 * Поддерживает сезонные окна (например, летнее/зимнее расписание).
 */
public record TimeWindow(
    String name,
    Set<DayOfWeek> weekDays,
    LocalTime dayStart,
    LocalTime dayEnd,
    ZoneId zone,
    boolean includeHolidays,
    MonthDay seasonStart,
    MonthDay seasonEnd
) {
    /**
     * Создать стандартное 24/7 окно.
     */
    public static TimeWindow aroundTheClock(String name, ZoneId zone) {
        return new TimeWindow(
            name,
            Set.of(DayOfWeek.values()),
            LocalTime.MIN,
            LocalTime.MAX,
            zone,
            true,
            null,
            null
        );
    }

    /**
     * Создать стандартное 8x5 окно.
     */
    public static TimeWindow businessHours(String name, ZoneId zone,
                                            LocalTime start, LocalTime end) {
        return new TimeWindow(
            name,
            Set.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                   DayOfWeek.THURSDAY, DayOfWeek.FRIDAY),
            start, end, zone, false, null, null
        );
    }

    /**
     * Продолжительность рабочего дня в секундах.
     */
    public long dailyDurationSeconds() {
        var start = dayStart;
        var end = dayEnd;
        if (end.equals(LocalTime.MAX)) {
            // 24/7 — 86400 секунд
            return 86400L;
        }
        java.time.Duration d = java.time.Duration.between(start, end);
        return d.isNegative() ? 0 : d.getSeconds();
    }

    /**
     * Проверить, что день является рабочим согласно этому окну.
     * @param dayOfWeek день недели
     * @param zoneTime время в зоне окна (для сравнения)
     */
    public boolean isDayWorking(DayOfWeek dayOfWeek) {
        return weekDays.contains(dayOfWeek);
    }

    /**
     * Проверить, что момент времени находится внутри рабочего окна.
     */
    public boolean isTimeInWindow(LocalTime time) {
        return !time.isBefore(dayStart) && !time.isAfter(dayEnd);
    }

    /**
     * Ограничить время рамками рабочего окна (clamp).
     */
    public LocalTime clampTime(LocalTime time) {
        if (time.isBefore(dayStart)) return dayStart;
        if (time.isAfter(dayEnd)) return dayEnd;
        return time;
    }
}
