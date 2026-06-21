package io.itsm.sla.model.action;

import io.itsm.sla.model.TimeWindow;

import java.time.Duration;
import java.util.List;

/**
 * Действие: дедлайн = startTime + duration, с учётом TimeWindow.
 * <p>
 * Основной тип SLA: задаёт длительность и расписание, когда время считается рабочим.
 */
public record DurationAction(
    Duration duration,
    List<TimeWindow> timeWindows,
    String precision
) implements SLAAction {

    public DurationAction {
        if (duration == null || duration.isNegative()) {
            throw new IllegalArgumentException("duration must be positive");
        }
        if (timeWindows == null || timeWindows.isEmpty()) {
            // Пустой список окон = 24/7 календарное время
            timeWindows = List.of();
        }
    }

    /**
     * Проверить, является ли SLA круглосуточным (24/7).
     */
    @com.fasterxml.jackson.annotation.JsonIgnore
    public boolean isAroundTheClock() {
        return timeWindows.isEmpty() || timeWindows.stream().anyMatch(tw ->
            tw.weekDays().size() == 7
                && tw.dayStart().equals(java.time.LocalTime.MIN)
                && tw.dayEnd().equals(java.time.LocalTime.MAX)
                && tw.includeHolidays()
        );
    }

    /**
     * Длительность в минутах.
     */
    @com.fasterxml.jackson.annotation.JsonIgnore
    public long durationMinutes() {
        return duration.toMinutes();
    }
}
