package io.itsm.sla.service;

import io.itsm.sla.calendar.BusinessDayCalendar;
import io.itsm.sla.model.*;
import io.itsm.sla.model.action.*;
import io.itsm.sla.port.SLARuleLoader;
import lombok.extern.slf4j.Slf4j;

import java.time.*;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Калькулятор SLA-дедлайнов — основной сервис.
 * <p>
 * 1. Выбирает подходящее SLA-правило по SLAContext
 * 2. Вычисляет дедлайн с учётом TimeWindow, праздников и DST
 */
@Slf4j
public class DeadlineCalculator {

    private final SLARuleLoader ruleLoader;
    private final BusinessDayCalendar calendar;

        /**
     * Основной конструктор — правила загружаются через {@link SLARuleLoader}.
     */
    public DeadlineCalculator(SLARuleLoader ruleLoader, BusinessDayCalendar calendar) {
        this.ruleLoader = ruleLoader;
        this.calendar = calendar;
    }

    /**
     * Конструктор для тестов — правила передаются списком напрямую.
     */
    public DeadlineCalculator(List<SLARule> rules, BusinessDayCalendar calendar) {
        this.ruleLoader = new SLARuleLoader() {
            @Override public void saveAll(List<SLARule> r) {}
            @Override public List<SLARule> loadAll() { return rules; }
        };
        this.calendar = calendar;
    }

    /**
     * Вычислить дедлайн для контекста обращения.
     *
     * @param context   контекст обращения
     * @param startTime время создания обращения
     * @return дедлайн
     */
    public SLADeadline computeDeadline(SLAContext context, ZonedDateTime startTime) {
        var rule = findRule(context)
            .orElseThrow(() -> new IllegalArgumentException(
                "No matching SLA rule for context: " + context));

        log.debug("Computing deadline for rule '{}' (priority {})", rule.id(), rule.priority());
        var deadline = computeActionDeadline(rule.action(), startTime);

        return SLADeadline.builder()
            .deadline(deadline)
            .slaRuleId(rule.id())
            .slaRuleName(rule.name())
            .durationMinutes(extractDurationMinutes(rule.action()))
            .compliant(true)
            .description(rule.description())
            .build();
    }

    /**
     * Вычислить дедлайн для произвольного действия.
     */
    private ZonedDateTime computeActionDeadline(SLAAction action, ZonedDateTime startTime) {
        return switch (action) {
            case DurationAction da -> computeDurationDeadline(da, startTime);
            case FixedDeadlineAction fa -> computeFixedDeadline(fa, startTime);
            case EscalateAction ea -> computeEscalateDeadline(ea, startTime);
            case CompositeAction ca -> computeCompositeDeadline(ca, startTime);
        };
    }

    /**
     * Вычисление дедлайна на основе длительности и окон времени.
     * <p>
     * Алгоритм:
     * - Если SLA 24/7 → просто прибавить duration к startTime
     * - Если рабочее время → продвигаться по дням, накапливая
     *   доступные минуты из каждого дня
     */
    private ZonedDateTime computeDurationDeadline(DurationAction action, ZonedDateTime startTime) {
        if (action.isAroundTheClock()) {
            return startTime.plus(action.duration());
        }

        var isBusinessDays = "DAYS".equalsIgnoreCase(action.precision());
        var remainingMinutes = (double) action.duration().toSeconds() / 60.0;
        var remainingBusinessDays = isBusinessDays ? action.duration().toDays() : 0;
        var current = startTime;
        var zone = startTime.getZone();

        while (isBusinessDays ? remainingBusinessDays > 0 : remainingMinutes > 0) {
            var tw = findActiveWindow(action.timeWindows(), current);
            if (tw.isEmpty()) {
                current = current.toLocalDate().plusDays(1).atStartOfDay(zone);
                continue;
            }

            var window = tw.get();
            var localDate = current.toLocalDate();
            var localTime = current.toLocalTime();

            if (localTime.isBefore(window.dayStart())) {
                current = localDate.atTime(window.dayStart()).atZone(zone);
                localTime = window.dayStart();
            }

            var zoneStr = zone.getId();
            if (!window.weekDays().contains(localDate.getDayOfWeek())
                || (!window.includeHolidays() && !calendar.isBusinessDay(localDate, zoneStr))) {
                current = localDate.plusDays(1).atStartOfDay(zone);
                continue;
            }

            var effectiveEnd = calendar.isShortenedDay(localDate, zoneStr)
                ? window.dayEnd().minusHours(1)
                : window.dayEnd();
            var dayEndTime = localDate.atTime(effectiveEnd).atZone(zone);

            if (current.isAfter(dayEndTime)) {
                current = localDate.plusDays(1).atStartOfDay(zone);
                continue;
            }

            if (isBusinessDays) {
                remainingBusinessDays--;
                if (remainingBusinessDays <= 0) {
                    return adjustForDST(dayEndTime, zone);
                }
                current = localDate.plusDays(1).atStartOfDay(zone);
            } else {
                var availableMinutes = Duration.between(current, dayEndTime).toMinutes();
                if (availableMinutes >= remainingMinutes) {
                    var deadlineInstant = current.plus(Duration.ofMinutes((long) remainingMinutes));
                    return adjustForDST(deadlineInstant, zone);
                }
                remainingMinutes -= availableMinutes;
                current = localDate.plusDays(1).atStartOfDay(zone);
            }
        }

        return current;
    }

    /**
     * Вычисление фиксированного дедлайна по выражению.
     */
    private ZonedDateTime computeFixedDeadline(FixedDeadlineAction action, ZonedDateTime startTime) {
        var zone = action.zone() != null ? action.zone() : startTime.getZone();

        return switch (action.detectType()) {
            case LAST_BUSINESS_DAY -> computeLastBusinessDay(startTime, zone);
            case NEXT_WEEKDAY -> computeNextWeekday(action.expression(), startTime, zone);
            case END_OF_NEXT_DAY -> computeEndOfNextBusinessDay(startTime, zone);
            case ABSOLUTE -> ZonedDateTime.parse(action.expression());
            case DURATION, CUSTOM ->
                // Для custom/deadline — применяем как DurationAction 24/7
                startTime.plus(Duration.parse(action.expression()));
        };
    }

    /**
     * Вычисление эскалационного дедлайна.
     */
    private ZonedDateTime computeEscalateDeadline(EscalateAction action, ZonedDateTime startTime) {
        // Сначала вычисляем дедлайн для эскалационного действия
        return computeActionDeadline(action.escalateTo(), startTime);
    }

    /**
     * Композитный дедлайн: применяем первое действие.
     */
    private ZonedDateTime computeCompositeDeadline(CompositeAction action, ZonedDateTime startTime) {
        return computeActionDeadline(action.first(), startTime);
    }

    // ---- вспомогательные методы ----

    private Optional<SLARule> findRule(SLAContext context) {
        return ruleLoader.loadAll().stream()
            .filter(SLARule::active)
            .sorted(Comparator.comparingInt(SLARule::priority))
            .filter(rule -> rule.matches(context))
            .findFirst();
    }

    private Optional<TimeWindow> findActiveWindow(List<TimeWindow> windows, ZonedDateTime moment) {
        if (windows.isEmpty()) {
            return Optional.empty();
        }
        // Выбираем окно, которое покрывает текущий сезон (если сезонные окна заданы)
        var monthDay = MonthDay.from(moment);
        return windows.stream()
            .filter(tw -> tw.seasonStart() == null || tw.seasonEnd() == null
                || isInSeason(monthDay, tw.seasonStart(), tw.seasonEnd()))
            .findFirst();
    }

    private boolean isInSeason(MonthDay current, MonthDay start, MonthDay end) {
        // Поддержка сезонов, переходящих через границу года (например, ноябрь-март)
        if (start.isBefore(end) || start.equals(end)) {
            return !current.isBefore(start) && !current.isAfter(end);
        } else {
            // Сезон переходит через год: start .. 31.12, 01.01 .. end
            return !current.isBefore(start) || !current.isAfter(end);
        }
    }

    private ZonedDateTime adjustForDST(ZonedDateTime dateTime, ZoneId zone) {
        // DST-safe: ZonedDateTime уже учитывает DST, но после расчётов
        // сдвиг может быть на несуществующее время — Spring/autumn DST transition
        try {
            return dateTime.withZoneSameInstant(zone);
        } catch (Exception e) {
            log.warn("DST adjustment failed for {}, using original", dateTime);
            return dateTime;
        }
    }

    private ZonedDateTime computeLastBusinessDay(ZonedDateTime startTime, ZoneId zone) {
        var yearMonth = YearMonth.from(startTime);
        var lastDay = yearMonth.atEndOfMonth();
        // Идём назад, пока не найдём рабочий день
        while (!calendar.isBusinessDay(lastDay, zone.getId())) {
            lastDay = lastDay.minusDays(1);
        }
        return lastDay.atTime(LocalTime.of(18, 0)).atZone(zone);
    }

    private ZonedDateTime computeNextWeekday(String expression, ZonedDateTime startTime, ZoneId zone) {
        // "next-monday 09:00"
        var parts = expression.split("\\s+");
        if (parts.length < 2) {
            return startTime.plusDays(1);
        }
        var dayStr = parts[0].replace("next-", "").toUpperCase();
        var timeStr = parts[1];

        try {
            var targetDay = DayOfWeek.valueOf(dayStr);
            var targetTime = LocalTime.parse(timeStr);
            var current = startTime.toLocalDate();
            var daysUntil = (targetDay.getValue() - current.getDayOfWeek().getValue() + 7) % 7;
            if (daysUntil == 0) daysUntil = 7; // следующий, не текущий
            return current.plusDays(daysUntil).atTime(targetTime).atZone(zone);
        } catch (IllegalArgumentException e) {
            return startTime.plusDays(1);
        }
    }

    private ZonedDateTime computeEndOfNextBusinessDay(ZonedDateTime startTime, ZoneId zone) {
        var nextDay = calendar.nextBusinessDay(startTime.toLocalDate(), zone);
        return nextDay.atTime(LocalTime.of(18, 0)).atZone(zone);
    }

    private long extractDurationMinutes(SLAAction action) {
        return switch (action) {
            case DurationAction da -> da.duration().toMinutes();
            case EscalateAction ea -> ea.initialDuration().toMinutes();
            case CompositeAction ca -> extractDurationMinutes(ca.first());
            case FixedDeadlineAction fa -> 0;
        };
    }
}
