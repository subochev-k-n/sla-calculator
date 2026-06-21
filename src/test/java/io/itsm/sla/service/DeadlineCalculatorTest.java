package io.itsm.sla.service;

import io.itsm.sla.calendar.BusinessDayCalendar;
import io.itsm.sla.calendar.Holiday;
import io.itsm.sla.model.*;
import io.itsm.sla.model.action.DurationAction;
import io.itsm.sla.model.condition.AndCondition;
import io.itsm.sla.model.condition.AttributeCondition;
import io.itsm.sla.model.condition.SimpleCondition;
import io.itsm.sla.model.condition.TrueCondition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.*;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты для DeadlineCalculator.
 */
class DeadlineCalculatorTest {

    private DeadlineCalculator calculator;

    private BusinessDayCalendar calendar;

    @BeforeEach
    void setUp() {
        calendar = new BusinessDayCalendar();
        // Московские праздники на 2026 год
        calendar.initHolidays(List.of(
            new Holiday(LocalDate.of(2026, 1, 1), "Новый год", Holiday.HolidayType.PUBLIC, ZoneId.of("Europe/Moscow")),
            new Holiday(LocalDate.of(2026, 1, 2), "Новый год (2)", Holiday.HolidayType.PUBLIC, ZoneId.of("Europe/Moscow")),
            new Holiday(LocalDate.of(2026, 1, 7), "Рождество", Holiday.HolidayType.PUBLIC, ZoneId.of("Europe/Moscow")),
            new Holiday(LocalDate.of(2026, 2, 23), "День защитника Отечества", Holiday.HolidayType.PUBLIC, ZoneId.of("Europe/Moscow")),
            new Holiday(LocalDate.of(2026, 3, 8), "Международный женский день", Holiday.HolidayType.PUBLIC, ZoneId.of("Europe/Moscow")),
            new Holiday(LocalDate.of(2026, 5, 1), "Праздник Весны и Труда", Holiday.HolidayType.PUBLIC, ZoneId.of("Europe/Moscow")),
            new Holiday(LocalDate.of(2026, 5, 9), "День Победы", Holiday.HolidayType.PUBLIC, ZoneId.of("Europe/Moscow")),
            new Holiday(LocalDate.of(2026, 6, 12), "День России", Holiday.HolidayType.PUBLIC, ZoneId.of("Europe/Moscow")),
            new Holiday(LocalDate.of(2026, 11, 4), "День народного единства", Holiday.HolidayType.PUBLIC, ZoneId.of("Europe/Moscow"))
        ));

        var incidentCritical = new SLARule(
            "incident-critical", "Критичный инцидент", "P1 4h 24/7",
            1,
            new AndCondition(List.of(
                new SimpleCondition("ticketType", "incident"),
                new AttributeCondition("urgency", AttributeCondition.Operator.EQ, "critical")
            )),
            new DurationAction(
                Duration.ofHours(4),
                List.of(TimeWindow.aroundTheClock("24/7", ZoneId.of("Europe/Moscow"))),
                "MINUTES"
            ),
            true
        );

        var incidentBusiness = new SLARule(
            "incident-business", "Бизнес-инцидент", "8h рабочее время",
            2,
            new SimpleCondition("ticketType", "incident"),
            new DurationAction(
                Duration.ofHours(8),
                List.of(TimeWindow.businessHours("8x5", ZoneId.of("Europe/Moscow"),
                    LocalTime.of(9, 0), LocalTime.of(18, 0))),
                "HOURS"
            ),
            true
        );

        var defaultRule = new SLARule(
            "default", "Default", "24h календарные",
            999,
            new TrueCondition(),
            new DurationAction(
                Duration.ofHours(24),
                List.of(TimeWindow.aroundTheClock("24/7", ZoneId.of("Europe/Moscow"))),
                "HOURS"
            ),
            true
        );

        calculator = new DeadlineCalculator(
            List.of(incidentCritical, incidentBusiness, defaultRule),
            calendar
        );
    }

    @Nested
    @DisplayName("24/7 SLA — календарное время")
    class Calendar247 {

        @Test
        @DisplayName("P1 инцидент: deadline = start + 4 часа")
        void p1DeadlineSimple() {
            var ctx = SLAContext.builder()
                .ticketType("incident")
                .attributes(Map.of("urgency", "critical"))
                .build();
            var start = ZonedDateTime.of(2026, 6, 15, 10, 0, 0, 0, ZoneId.of("Europe/Moscow"));

            var result = calculator.computeDeadline(ctx, start);

            assertEquals(
                ZonedDateTime.of(2026, 6, 15, 14, 0, 0, 0, ZoneId.of("Europe/Moscow")),
                result.deadline()
            );
            assertEquals("incident-critical", result.slaRuleId());
        }

        @Test
        @DisplayName("P1 инцидент через границу дней: deadline на следующий день")
        void p1DeadlineCrossMidnight() {
            var ctx = SLAContext.builder()
                .ticketType("incident")
                .attributes(Map.of("urgency", "critical"))
                .build();
            var start = ZonedDateTime.of(2026, 6, 15, 22, 0, 0, 0, ZoneId.of("Europe/Moscow"));

            var result = calculator.computeDeadline(ctx, start);

            assertEquals(
                ZonedDateTime.of(2026, 6, 16, 2, 0, 0, 0, ZoneId.of("Europe/Moscow")),
                result.deadline()
            );
        }
    }

    @Nested
    @DisplayName("Рабочее время 8x5 — бизнес-часы")
    class BusinessHours {

        @Test
        @DisplayName("В понедельник 10:00 + 8 рабочих часов = понедельник 18:00")
        void mondayMorning() {
            var ctx = SLAContext.builder()
                .ticketType("incident")
                .attributes(Map.of("urgency", "high"))
                .build();
            var start = ZonedDateTime.of(2026, 6, 15, 10, 0, 0, 0, ZoneId.of("Europe/Moscow"));

            var result = calculator.computeDeadline(ctx, start);

            assertEquals(
                ZonedDateTime.of(2026, 6, 15, 18, 0, 0, 0, ZoneId.of("Europe/Moscow")),
                result.deadline()
            );
        }

        @Test
        @DisplayName("В пятницу 16:00 + 8 рабочих часов = понедельник 14:00 (с учётом выходных)")
        void fridayAfternoon() {
            var ctx = SLAContext.builder()
                .ticketType("incident")
                .attributes(Map.of("urgency", "high"))
                .build();
            // Пятница 16:00 — до конца дня 2 часа
            var start = ZonedDateTime.of(2026, 6, 19, 16, 0, 0, 0, ZoneId.of("Europe/Moscow"));

            var result = calculator.computeDeadline(ctx, start);

            // 2 часа в пятницу + 6 часов в понедельник = Пн 15:00
            assertEquals(
                ZonedDateTime.of(2026, 6, 22, 15, 0, 0, 0, ZoneId.of("Europe/Moscow")),
                result.deadline()
            );
        }

        @Test
        @DisplayName("В пятницу 9:00 + 8 часов = пятница 17:00 (уложились в день)")
        void fridayFullDay() {
            var ctx = SLAContext.builder()
                .ticketType("incident")
                .attributes(Map.of("urgency", "high"))
                .build();
            var start = ZonedDateTime.of(2026, 6, 19, 9, 0, 0, 0, ZoneId.of("Europe/Moscow"));

            var result = calculator.computeDeadline(ctx, start);

            assertEquals(
                ZonedDateTime.of(2026, 6, 19, 17, 0, 0, 0, ZoneId.of("Europe/Moscow")),
                result.deadline()
            );
        }

        @Test
        @DisplayName("Перед праздником 22.02 + 8 часов = выходные + праздник 23.02 вторник")
        void beforeHoliday() {
            // Инициализируем календарь с праздником 23 февраля 2026
            // 23.02.2026 — понедельник? нет, давайте проверим
            // 2026-02-23 — понедельник
            var ctx = SLAContext.builder()
                .ticketType("incident")
                .attributes(Map.of("urgency", "high"))
                .build();
            // Пятница 20.02.2026 16:00 + 8 рабочих часов
            var start = ZonedDateTime.of(2026, 2, 20, 16, 0, 0, 0, ZoneId.of("Europe/Moscow"));

            var result = calculator.computeDeadline(ctx, start);

            // 20.02 пятница: с 16:00 до 18:00 = 2 часа
            // 21.02 СБ — выходной
            // 22.02 ВС — выходной
            // 23.02 ПН — праздник (нерабочий)
            // 24.02 ВТ — рабочий: 6 оставшихся часов → 9:00 + 6 = 15:00
            assertEquals(
                ZonedDateTime.of(2026, 2, 24, 15, 0, 0, 0, ZoneId.of("Europe/Moscow")),
                result.deadline()
            );
        }
    }

    @Nested
    @DisplayName("Default SLA — fallback")
    class DefaultRule {

        @Test
        @DisplayName("Неизвестный тип обращения → default 24h")
        void unknownTicketType() {
            var ctx = SLAContext.builder()
                .ticketType("unknown_type")
                .build();
            var start = ZonedDateTime.of(2026, 6, 15, 10, 0, 0, 0, ZoneId.of("Europe/Moscow"));

            var result = calculator.computeDeadline(ctx, start);

            assertEquals(
                ZonedDateTime.of(2026, 6, 16, 10, 0, 0, 0, ZoneId.of("Europe/Moscow")),
                result.deadline()
            );
            assertEquals("default", result.slaRuleId());
        }
    }

    @Nested
    @DisplayName("DST-переходы")
    class DSTTransitions {

        @Test
        @DisplayName("Расчёт через весенний DST (переход на летнее время)")
        void springDST() {
            var ctx = SLAContext.builder()
                .ticketType("incident")
                .attributes(Map.of("urgency", "critical"))
                .build();
            // За час до перехода (в 2026 DST весной ~ 29 марта)
            var start = ZonedDateTime.of(2026, 3, 29, 1, 0, 0, 0, ZoneId.of("Europe/Moscow"));

            var result = calculator.computeDeadline(ctx, start);

            // Moscow MSK не переходит на DST с 2014 года, но проверим общую логику
            assertNotNull(result.deadline());
            assertEquals("incident-critical", result.slaRuleId());
        }
    }

    @Nested
    @DisplayName("Високосный год")
    class LeapYear {

        @Test
        @DisplayName("Расчёт в високосный 2028 год — некорректная дата не возникает")
        void leapYearCalculation() {
            var ctx = SLAContext.builder()
                .ticketType("incident")
                .attributes(Map.of("urgency", "critical"))
                .build();
            var start = ZonedDateTime.of(2028, 2, 28, 10, 0, 0, 0, ZoneId.of("Europe/Moscow"));

            var result = calculator.computeDeadline(ctx, start);

            // deadline = 28.02.2028 14:00 (4 часа)
            assertEquals(
                ZonedDateTime.of(2028, 2, 28, 14, 0, 0, 0, ZoneId.of("Europe/Moscow")),
                result.deadline()
            );
        }
    }
}
