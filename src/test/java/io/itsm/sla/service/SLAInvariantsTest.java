package io.itsm.sla.service;

import io.itsm.sla.calendar.BusinessDayCalendar;
import io.itsm.sla.calendar.Holiday;
import io.itsm.sla.model.*;
import io.itsm.sla.model.action.DurationAction;
import io.itsm.sla.model.condition.AttributeCondition;
import io.itsm.sla.model.condition.SimpleCondition;
import io.itsm.sla.model.condition.TrueCondition;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;

import java.time.*;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based тесты для проверки инвариантов SLA-калькулятора.
 */
class SLAInvariantsTest {

    private static final ZoneId MSK = ZoneId.of("Europe/Moscow");

    private DeadlineCalculator createCalculator() {
        var calendar = new BusinessDayCalendar();
        calendar.initHolidays(List.of(
            new Holiday(LocalDate.of(2026, 1, 1), "New Year", Holiday.HolidayType.PUBLIC, MSK),
            new Holiday(LocalDate.of(2026, 5, 1), "Labour Day", Holiday.HolidayType.PUBLIC, MSK),
            new Holiday(LocalDate.of(2026, 5, 9), "Victory Day", Holiday.HolidayType.PUBLIC, MSK)
        ));

        var rule247 = new SLARule(
            "24x7", "24/7", "4h 24/7",
            1,
            new TrueCondition(),
            new DurationAction(Duration.ofHours(4),
                List.of(TimeWindow.aroundTheClock("24/7", MSK)), "MINUTES"),
            true
        );

        var ruleBusiness = new SLARule(
            "8x5", "Business hours", "8h working time",
            2,
            new SimpleCondition("ticketType", "business"),
            new DurationAction(Duration.ofHours(8),
                List.of(TimeWindow.businessHours("8x5", MSK,
                    LocalTime.of(9, 0), LocalTime.of(18, 0))), "HOURS"),
            true
        );

        return new DeadlineCalculator(List.of(rule247, ruleBusiness), calendar);
    }

    @Property
    @Label("Инвариант 24/7: deadline всегда после startTime")
    void deadlineAlwaysAfterStart247(
            @ForAll @IntRange(min = 2024, max = 2030) int year,
            @ForAll @IntRange(min = 1, max = 365) int dayOfYear,
            @ForAll @IntRange(min = 0, max = 23) int hour,
            @ForAll @IntRange(min = 0, max = 59) int minute
    ) {
        var calculator = createCalculator();

        var date = LocalDate.ofYearDay(year, dayOfYear);
        // Пропускаем 29 февраля невисокосных лет
        if (date.getMonth() == Month.FEBRUARY && date.getDayOfMonth() == 29
            && !Year.of(year).isLeap()) {
            return;
        }

        var startTime = date.atTime(hour, minute).atZone(MSK);
        var ctx = SLAContext.builder()
            .ticketType("test")
            .attributes(Map.of("urgency", "critical"))
            .build();

        var result = calculator.computeDeadline(ctx, startTime);

        assertNotNull(result.deadline());
        assertTrue(result.deadline().isAfter(startTime),
            () -> "deadline " + result.deadline() + " должен быть после start " + startTime);
    }

    @Property
    @Label("Инвариант: результат всегда содержит slaRuleId")
    void resultAlwaysHasRuleId(
            @ForAll String ticketType
    ) {
        // Фильтр: пропускаем пустые типы
        Assume.that(ticketType != null && !ticketType.isBlank());

        var calculator = createCalculator();
        var startTime = ZonedDateTime.now();
        var ctx = SLAContext.builder()
            .ticketType(ticketType)
            .build();

        var result = calculator.computeDeadline(ctx, startTime);

        assertNotNull(result);
        assertNotNull(result.slaRuleId(),
            "для ticketType=" + ticketType + " должен быть выбран ruleId");
    }

    @Property
    @Label("Инвариант: durationMinutes консистентна с duration в правиле")
    void durationConsistentWithRule(
            @ForAll String ticketType,
            @ForAll @IntRange(min = 1, max = 10000) int extraMinutes
    ) {
        var calculator = createCalculator();
        var startTime = ZonedDateTime.now();
        var ctx = SLAContext.builder()
            .ticketType(ticketType)
            .build();

        var result = calculator.computeDeadline(ctx, startTime);

        assertTrue(result.durationMinutes() > 0,
            "durationMinutes должен быть > 0 для ticketType=" + ticketType);
    }

    @Provide
    List<TimeWindow> validTimeWindows() {
        return List.of(
            TimeWindow.aroundTheClock("24/7", MSK),
            TimeWindow.businessHours("8x5", MSK, LocalTime.of(9, 0), LocalTime.of(18, 0)),
            TimeWindow.businessHours("night", MSK, LocalTime.of(22, 0), LocalTime.of(6, 0))
        );
    }

    @Property
    @Label("Инвариант: calendar корректно различает рабочие/нерабочие дни")
    void calendarConsistency() {
        // Проверка, что BusinessDayCalendar корректно различает рабочие/нерабочие дни
        var calendar = new BusinessDayCalendar();
        calendar.initHolidays(List.of(
            new Holiday(LocalDate.of(2026, 1, 1), "New Year", Holiday.HolidayType.PUBLIC, MSK)
        ));

        assertFalse(calendar.isBusinessDay(LocalDate.of(2026, 1, 1), MSK.getId()), "1 Jan should be holiday");
        assertFalse(calendar.isBusinessDay(LocalDate.of(2026, 6, 20), MSK.getId()), "Saturday should be weekend");
        assertFalse(calendar.isBusinessDay(LocalDate.of(2026, 6, 21), MSK.getId()), "Sunday should be weekend");
        assertTrue(calendar.isBusinessDay(LocalDate.of(2026, 6, 22), MSK.getId()), "Monday should be business day");
    }
}
