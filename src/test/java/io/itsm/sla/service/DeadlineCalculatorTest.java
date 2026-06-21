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

import static org.junit.jupiter.api.Assertions.*;

class DeadlineCalculatorTest {

    private DeadlineCalculator calculator;
    private BusinessDayCalendar calendar;

    @BeforeEach
    void setUp() {
        calendar = new BusinessDayCalendar();
        calendar.initHolidays(List.of(
            new Holiday(LocalDate.of(2026, 1, 1), "Новый год", Holiday.HolidayType.PUBLIC, ZoneId.of("Europe/Moscow")),
            new Holiday(LocalDate.of(2026, 1, 2), "Новый год (2)", Holiday.HolidayType.PUBLIC, ZoneId.of("Europe/Moscow")),
            new Holiday(LocalDate.of(2026, 1, 3), "Новый год (3)", Holiday.HolidayType.PUBLIC, ZoneId.of("Europe/Moscow")),
            new Holiday(LocalDate.of(2026, 1, 4), "Новый год (4)", Holiday.HolidayType.PUBLIC, ZoneId.of("Europe/Moscow")),
            new Holiday(LocalDate.of(2026, 1, 5), "Новый год (5)", Holiday.HolidayType.PUBLIC, ZoneId.of("Europe/Moscow")),
            new Holiday(LocalDate.of(2026, 1, 6), "Новый год (6)", Holiday.HolidayType.PUBLIC, ZoneId.of("Europe/Moscow")),
            new Holiday(LocalDate.of(2026, 1, 7), "Рождество", Holiday.HolidayType.PUBLIC, ZoneId.of("Europe/Moscow")),
            new Holiday(LocalDate.of(2026, 2, 23), "День защитника Отечества", Holiday.HolidayType.PUBLIC, ZoneId.of("Europe/Moscow")),
            new Holiday(LocalDate.of(2026, 3, 8), "Международный женский день", Holiday.HolidayType.PUBLIC, ZoneId.of("Europe/Moscow")),
            new Holiday(LocalDate.of(2026, 5, 1), "Праздник Весны и Труда", Holiday.HolidayType.PUBLIC, ZoneId.of("Europe/Moscow")),
            new Holiday(LocalDate.of(2026, 5, 9), "День Победы", Holiday.HolidayType.PUBLIC, ZoneId.of("Europe/Moscow")),
            new Holiday(LocalDate.of(2026, 6, 12), "День России", Holiday.HolidayType.PUBLIC, ZoneId.of("Europe/Moscow")),
            new Holiday(LocalDate.of(2026, 11, 4), "День народного единства", Holiday.HolidayType.PUBLIC, ZoneId.of("Europe/Moscow"))
        ));

        var p1 = new SLARule("p1", "P1", "4h 24/7", 1,
            new AndCondition(List.of(
                new SimpleCondition("ticketType", "incident"),
                new AttributeCondition("urgency", AttributeCondition.Operator.EQ, "critical"))),
            new DurationAction(Duration.ofHours(4),
                List.of(TimeWindow.aroundTheClock("24/7", ZoneId.of("Europe/Moscow"))), "MINUTES"), true);

        var p2 = new SLARule("p2", "P2", "8h рабочее", 2,
            new SimpleCondition("ticketType", "incident"),
            new DurationAction(Duration.ofHours(8),
                List.of(TimeWindow.businessHours("8x5", ZoneId.of("Europe/Moscow"),
                    LocalTime.of(9, 0), LocalTime.of(18, 0))), "HOURS"), true);

        var d5 = new SLARule("d5", "5дн", "5 рабочих дней", 3,
            new SimpleCondition("ticketType", "d5"),
            new DurationAction(Duration.ofDays(5),
                List.of(TimeWindow.businessHours("8x5", ZoneId.of("Europe/Moscow"),
                    LocalTime.of(9, 0), LocalTime.of(18, 0))), "DAYS"), true);

        var d3 = new SLARule("d3", "3дн", "3 рабочих дня", 4,
            new SimpleCondition("ticketType", "d3"),
            new DurationAction(Duration.ofDays(3),
                List.of(TimeWindow.businessHours("8x5", ZoneId.of("Europe/Moscow"),
                    LocalTime.of(9, 0), LocalTime.of(18, 0))), "DAYS"), true);

        var d2 = new SLARule("d2", "2дн", "2 рабочих дня", 5,
            new SimpleCondition("ticketType", "d2"),
            new DurationAction(Duration.ofDays(2),
                List.of(TimeWindow.businessHours("8x5", ZoneId.of("Europe/Moscow"),
                    LocalTime.of(9, 0), LocalTime.of(18, 0))), "DAYS"), true);

        var d1 = new SLARule("d1", "1дн", "1 рабочий день", 6,
            new SimpleCondition("ticketType", "d1"),
            new DurationAction(Duration.ofDays(1),
                List.of(TimeWindow.businessHours("8x5", ZoneId.of("Europe/Moscow"),
                    LocalTime.of(9, 0), LocalTime.of(18, 0))), "DAYS"), true);

        var def = new SLARule("default", "Default", "24h", 999, new TrueCondition(),
            new DurationAction(Duration.ofHours(24),
                List.of(TimeWindow.aroundTheClock("24/7", ZoneId.of("Europe/Moscow"))), "HOURS"), true);

        calculator = new DeadlineCalculator(
            List.of(p1, p2, d5, d3, d2, d1, def), calendar);
    }

    @Nested @DisplayName("24/7")
    class Calendar247 {
        @Test void p1Adds4Hours() {
            var r = calculator.computeDeadline(SLAContext.builder().ticketType("incident")
                .attributes(Map.of("urgency","critical")).build(),
                ZonedDateTime.of(2026,6,15,10,0,0,0,ZoneId.of("Europe/Moscow")));
            assertEquals(ZonedDateTime.of(2026,6,15,14,0,0,0,ZoneId.of("Europe/Moscow")), r.deadline());
        }
        @Test void p1CrossMidnight() {
            var r = calculator.computeDeadline(SLAContext.builder().ticketType("incident")
                .attributes(Map.of("urgency","critical")).build(),
                ZonedDateTime.of(2026,6,15,22,0,0,0,ZoneId.of("Europe/Moscow")));
            assertEquals(ZonedDateTime.of(2026,6,16,2,0,0,0,ZoneId.of("Europe/Moscow")), r.deadline());
        }
    }

    @Nested @DisplayName("Рабочие часы 8x5")
    class BusinessHours {
        @Test void monday10to18() {
            var r = calculator.computeDeadline(SLAContext.builder().ticketType("incident")
                .attributes(Map.of("urgency","high")).build(),
                ZonedDateTime.of(2026,6,15,10,0,0,0,ZoneId.of("Europe/Moscow")));
            assertEquals(ZonedDateTime.of(2026,6,15,18,0,0,0,ZoneId.of("Europe/Moscow")), r.deadline());
        }
        @Test void friday16toMonday15() {
            // Пт 16:00 + 8ч = Пт 2ч (до 18) + Пн 6ч = Пн 15:00
            var r = calculator.computeDeadline(SLAContext.builder().ticketType("incident")
                .attributes(Map.of("urgency","high")).build(),
                ZonedDateTime.of(2026,6,19,16,0,0,0,ZoneId.of("Europe/Moscow")));
            assertEquals(ZonedDateTime.of(2026,6,22,15,0,0,0,ZoneId.of("Europe/Moscow")), r.deadline());
        }
        @Test void friday9to17() {
            var r = calculator.computeDeadline(SLAContext.builder().ticketType("incident")
                .attributes(Map.of("urgency","high")).build(),
                ZonedDateTime.of(2026,6,19,9,0,0,0,ZoneId.of("Europe/Moscow")));
            assertEquals(ZonedDateTime.of(2026,6,19,17,0,0,0,ZoneId.of("Europe/Moscow")), r.deadline());
        }
    }

    @Nested @DisplayName("Рабочие дни")
    class BusinessDays {
        @Test void monPlus5days_friday() {
            // Пн 15.06 + 5 р.д. = Пт 19.06 18:00
            var r = calculator.computeDeadline(SLAContext.builder().ticketType("d5").build(),
                ZonedDateTime.of(2026,6,15,10,0,0,0,ZoneId.of("Europe/Moscow")));
            assertEquals(ZonedDateTime.of(2026,6,19,18,0,0,0,ZoneId.of("Europe/Moscow")), r.deadline());
        }
        @Test void fri16plus5days_thu() {
            // Пт 19.06 + 5 р.д.: Пт(1)→Пн(2)→Вт(3)→Ср(4)→Чт(5) 18:00
            var r = calculator.computeDeadline(SLAContext.builder().ticketType("d5").build(),
                ZonedDateTime.of(2026,6,19,16,0,0,0,ZoneId.of("Europe/Moscow")));
            assertEquals(ZonedDateTime.of(2026,6,25,18,0,0,0,ZoneId.of("Europe/Moscow")), r.deadline());
        }
        @Test void feb20plus5days_feb27() {
            // Пт 20.02 + 5 р.д.: Пт(1)→праздник23→Вт24(2)→Ср25(3)→Чт26(4)→Пт27(5)
            var r = calculator.computeDeadline(SLAContext.builder().ticketType("d5").build(),
                ZonedDateTime.of(2026,2,20,14,0,0,0,ZoneId.of("Europe/Moscow")));
            assertEquals(ZonedDateTime.of(2026,2,27,18,0,0,0,ZoneId.of("Europe/Moscow")), r.deadline());
        }
        @Test void dec30plus5days_jan12() {
            // Вт 30.12 + 5 р.д.: Вт(1)→Ср(2)→прздн→Чт08(3)→Пт09(4)→Пн12(5)
            var r = calculator.computeDeadline(SLAContext.builder().ticketType("d5").build(),
                ZonedDateTime.of(2025,12,30,10,0,0,0,ZoneId.of("Europe/Moscow")));
            assertEquals(ZonedDateTime.of(2026,1,12,18,0,0,0,ZoneId.of("Europe/Moscow")), r.deadline());
        }
        @Test void fri16plus1day_sameDay() {
            // Пт 16:00 + 1 р.д. = Пт 18:00 (в тот же день, до 18:00)
            var r = calculator.computeDeadline(SLAContext.builder().ticketType("d1").build(),
                ZonedDateTime.of(2026,6,19,16,0,0,0,ZoneId.of("Europe/Moscow")));
            assertEquals(ZonedDateTime.of(2026,6,19,18,0,0,0,ZoneId.of("Europe/Moscow")), r.deadline());
        }
        @Test void friAfter18plus1day_monday() {
            // Пт 19:00 + 1 р.д. = Пн 22.06 18:00 (пт после 18 — пропуск, пн — 1й день)
            var r = calculator.computeDeadline(SLAContext.builder().ticketType("d1").build(),
                ZonedDateTime.of(2026,6,19,19,0,0,0,ZoneId.of("Europe/Moscow")));
            assertEquals(ZonedDateTime.of(2026,6,22,18,0,0,0,ZoneId.of("Europe/Moscow")), r.deadline());
        }
        @Test void wedAfternoonPlus3days_friday() {
            // Ср 17.06 15:30 + 3 р.д.: Ср(1)→Чт(2)→Пт(3) 18:00
            var r = calculator.computeDeadline(SLAContext.builder().ticketType("d3").build(),
                ZonedDateTime.of(2026,6,17,15,30,0,0,ZoneId.of("Europe/Moscow")));
            assertEquals(ZonedDateTime.of(2026,6,19,18,0,0,0,ZoneId.of("Europe/Moscow")), r.deadline());
        }
    }

    @Nested @DisplayName("Краевые случаи и праздники")
    class EdgeCases {
        @Test void exactly9to17() {
            var r = calculator.computeDeadline(SLAContext.builder().ticketType("incident")
                .attributes(Map.of("urgency","high")).build(),
                ZonedDateTime.of(2026,6,15,9,0,0,0,ZoneId.of("Europe/Moscow")));
            assertEquals(ZonedDateTime.of(2026,6,15,17,0,0,0,ZoneId.of("Europe/Moscow")), r.deadline());
        }
        @Test void exactly18toNextDay17() {
            var r = calculator.computeDeadline(SLAContext.builder().ticketType("incident")
                .attributes(Map.of("urgency","high")).build(),
                ZonedDateTime.of(2026,6,15,18,0,0,0,ZoneId.of("Europe/Moscow")));
            assertEquals(ZonedDateTime.of(2026,6,16,17,0,0,0,ZoneId.of("Europe/Moscow")), r.deadline());
        }
        @Test void sunday247() {
            var r = calculator.computeDeadline(SLAContext.builder().ticketType("incident")
                .attributes(Map.of("urgency","critical")).build(),
                ZonedDateTime.of(2026,6,21,14,0,0,0,ZoneId.of("Europe/Moscow")));
            assertEquals(ZonedDateTime.of(2026,6,21,18,0,0,0,ZoneId.of("Europe/Moscow")), r.deadline());
        }
        @Test void saturday247() {
            var r = calculator.computeDeadline(SLAContext.builder().ticketType("incident")
                .attributes(Map.of("urgency","critical")).build(),
                ZonedDateTime.of(2026,6,20,12,0,0,0,ZoneId.of("Europe/Moscow")));
            assertEquals(ZonedDateTime.of(2026,6,20,16,0,0,0,ZoneId.of("Europe/Moscow")), r.deadline());
        }
        @Test void mayHolidays() {
            // Чт 30.04 + 3 р.д.: Чт(1)→Птпраздник→Пн04(2)→Вт05(3) 18:00
            var r = calculator.computeDeadline(SLAContext.builder().ticketType("d3").build(),
                ZonedDateTime.of(2026,4,30,10,0,0,0,ZoneId.of("Europe/Moscow")));
            assertEquals(ZonedDateTime.of(2026,5,5,18,0,0,0,ZoneId.of("Europe/Moscow")), r.deadline());
        }
        @Test void novemberHoliday() {
            // Вт 03.11 + 2 р.д.: Вт(1)→Српраздник→Чт05(2) 18:00
            var r = calculator.computeDeadline(SLAContext.builder().ticketType("d2").build(),
                ZonedDateTime.of(2026,11,3,14,0,0,0,ZoneId.of("Europe/Moscow")));
            assertEquals(ZonedDateTime.of(2026,11,5,18,0,0,0,ZoneId.of("Europe/Moscow")), r.deadline());
        }
        @Test void unknownTypeDefault() {
            var r = calculator.computeDeadline(SLAContext.builder().ticketType("unknown").build(),
                ZonedDateTime.of(2026,6,15,10,0,0,0,ZoneId.of("Europe/Moscow")));
            assertEquals(ZonedDateTime.of(2026,6,16,10,0,0,0,ZoneId.of("Europe/Moscow")), r.deadline());
            assertEquals("default", r.slaRuleId());
        }
    }
}
