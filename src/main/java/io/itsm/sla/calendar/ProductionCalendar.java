package io.itsm.sla.calendar;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Year;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;

/**
 * Производственный календарь — интерфейс порта для получения информации
 * о праздниках и рабочих днях.
 */
public interface ProductionCalendar {

    /**
     * Получить все праздники для указанного года и часового пояса.
     */
    List<Holiday> getHolidays(Year year, String zoneId);

    /**
     * Проверить, является ли день рабочим (не выходной, не праздник).
     */
    boolean isBusinessDay(LocalDate date, String zoneId);

    /**
     * Получить следующий рабочий день после указанной даты.
     */
    LocalDate nextBusinessDay(LocalDate date, ZoneId zoneId);

    /**
     * Получить выходные дни (обычно СБ, ВС).
     */
    Set<DayOfWeek> getWeekendDays();

    /**
     * Проверить, является ли день сокращённым (предпраздничным).
     */
    boolean isShortenedDay(LocalDate date, String zoneId);
}
