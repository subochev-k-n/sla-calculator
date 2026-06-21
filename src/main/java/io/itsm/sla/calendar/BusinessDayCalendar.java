package io.itsm.sla.calendar;

import lombok.extern.slf4j.Slf4j;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Реализация производственного календаря.
 * <p>
 * Учитывает:
 * - Стандартные выходные (СБ, ВС)
 * - Государственные праздники
 * - Переносы праздников с выходных на рабочие дни
 * - Сокращённые предпраздничные дни
 * - Високосные годы
 * - Разные часовые пояса
 */
@Slf4j
public class BusinessDayCalendar implements ProductionCalendar {

    private static final Set<DayOfWeek> DEFAULT_WEEKEND = Set.of(
        DayOfWeek.SATURDAY, DayOfWeek.SUNDAY
    );

    private final Map<String, Map<Year, List<Holiday>>> holidayCache = new HashMap<>();

    @Override
    public List<Holiday> getHolidays(Year year, String zoneId) {
        return holidayCache.computeIfAbsent(zoneId, k -> new HashMap<>())
            .computeIfAbsent(year, y -> loadHolidays(y, zoneId));
    }

    @Override
    public boolean isBusinessDay(LocalDate date, String zoneId) {
        if (isWeekend(date)) return false;
        var holidays = getHolidays(Year.of(date.getYear()), zoneId);
        return holidays.stream()
            .noneMatch(h -> h.date().equals(date) && h.isNonWorking());
    }

    @Override
    public LocalDate nextBusinessDay(LocalDate date, ZoneId zoneId) {
        var zoneStr = zoneId.getId();
        var next = date.plusDays(1);
        while (!isBusinessDay(next, zoneStr)) {
            next = next.plusDays(1);
        }
        return next;
    }

    @Override
    public Set<DayOfWeek> getWeekendDays() {
        return DEFAULT_WEEKEND;
    }

    @Override
    public boolean isShortenedDay(LocalDate date, String zoneId) {
        var holidays = getHolidays(Year.of(date.getYear()), zoneId);
        return holidays.stream()
            .anyMatch(h -> h.date().equals(date) && h.isShortened());
    }

    /**
     * Получить первый рабочий момент дня: если день сокращённый,
     * то укоротить dayEnd на 1 час.
     */
    public LocalTime getEffectiveDayEnd(LocalDate date, LocalTime standardEnd, String zoneId) {
        if (isShortenedDay(date, zoneId)) {
            return standardEnd.minusHours(1);
        }
        return standardEnd;
    }

    /**
     * Получить количество рабочих минут в указанную дату
     * с учётом сокращённых дней.
     */
    public long getBusinessMinutesInDay(LocalDate date,
                                         LocalTime dayStart, LocalTime dayEnd,
                                         String zoneId) {
        if (!isBusinessDay(date, zoneId)) return 0L;

        var end = getEffectiveDayEnd(date, dayEnd, zoneId);
        var minutes = Duration.between(dayStart, end).toMinutes();
        return Math.max(0, minutes);
    }

    private boolean isWeekend(LocalDate date) {
        return DEFAULT_WEEKEND.contains(date.getDayOfWeek());
    }

    /**
     * Загрузить праздники (в реальном приложении — из БД или YAML).
     * Здесь метод-заглушка для инициализации из внешнего источника.
     */
    protected List<Holiday> loadHolidays(Year year, String zoneId) {
        // Будет переопределён в адаптере, читающем из YAML/БД
        return List.of();
    }

    /**
     * Инициализировать календарь списком праздников.
     */
    public void initHolidays(List<Holiday> holidays) {
        for (var h : holidays) {
            var zoneStr = h.zone().getId();
            var year = Year.of(h.date().getYear());
            var yearMap = holidayCache.computeIfAbsent(zoneStr, k -> new HashMap<>());
            yearMap.computeIfAbsent(year, y -> new ArrayList<>()).add(h);
        }
    }
}
