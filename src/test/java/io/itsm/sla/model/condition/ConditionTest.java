package io.itsm.sla.model.condition;

import io.itsm.sla.model.SLAContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты Condition Engine — дерево условий для выбора SLA-правила.
 */
class ConditionTest {

    private static final SLAContext INCIDENT_CRITICAL = SLAContext.builder()
        .ticketType("incident")
        .ticketCategory("system")
        .attributes(Map.of(
            "urgency", "critical",
            "impact", "high",
            "priority_score", "95"
        ))
        .build();

    private static final SLAContext SERVICE_REQUEST = SLAContext.builder()
        .ticketType("service_request")
        .ticketCategory("hardware")
        .attributes(Map.of(
            "urgency", "low",
            "cost_center", "IT-001"
        ))
        .build();

    @Nested
    @DisplayName("SimpleCondition")
    class SimpleConditionTests {

        @Test
        @DisplayName("Совпадение ticketType = incident")
        void ticketTypeMatch() {
            assertTrue(new SimpleCondition("ticketType", "incident").evaluate(INCIDENT_CRITICAL));
        }

        @Test
        @DisplayName("Несовпадение ticketType")
        void ticketTypeNoMatch() {
            assertFalse(new SimpleCondition("ticketType", "problem").evaluate(INCIDENT_CRITICAL));
        }

        @Test
        @DisplayName("Совпадение ticketCategory")
        void categoryMatch() {
            assertTrue(new SimpleCondition("ticketCategory", "system").evaluate(INCIDENT_CRITICAL));
        }
    }

    @Nested
    @DisplayName("AttributeCondition")
    class AttributeConditionTests {

        @Test
        @DisplayName("EQ — числовое равенство")
        void eqNumeric() {
            assertTrue(new AttributeCondition("priority_score", AttributeCondition.Operator.EQ, "95")
                .evaluate(INCIDENT_CRITICAL));
        }

        @Test
        @DisplayName("GTE — больше или равно")
        void gte() {
            assertTrue(new AttributeCondition("priority_score", AttributeCondition.Operator.GTE, "90")
                .evaluate(INCIDENT_CRITICAL));
            assertFalse(new AttributeCondition("priority_score", AttributeCondition.Operator.GTE, "99")
                .evaluate(INCIDENT_CRITICAL));
        }

        @Test
        @DisplayName("EXISTS — атрибут существует")
        void exists() {
            assertTrue(new AttributeCondition("urgency", AttributeCondition.Operator.EXISTS, "")
                .evaluate(INCIDENT_CRITICAL));
            assertFalse(new AttributeCondition("nonexistent", AttributeCondition.Operator.EXISTS, "")
                .evaluate(INCIDENT_CRITICAL));
        }

        @Test
        @DisplayName("IN — значение в списке")
        void in() {
            assertTrue(new AttributeCondition("urgency", AttributeCondition.Operator.IN, "critical,high,medium")
                .evaluate(INCIDENT_CRITICAL));
            assertFalse(new AttributeCondition("urgency", AttributeCondition.Operator.IN, "low,medium")
                .evaluate(INCIDENT_CRITICAL));
        }
    }

    @Nested
    @DisplayName("AndCondition")
    class AndConditionTests {

        @Test
        @DisplayName("Все условия истинны → true")
        void allTrue() {
            var condition = new AndCondition(List.of(
                new SimpleCondition("ticketType", "incident"),
                new AttributeCondition("urgency", AttributeCondition.Operator.EQ, "critical")
            ));
            assertTrue(condition.evaluate(INCIDENT_CRITICAL));
        }

        @Test
        @DisplayName("Хотя бы одно ложно → false")
        void oneFalse() {
            var condition = new AndCondition(List.of(
                new SimpleCondition("ticketType", "incident"),
                new SimpleCondition("ticketCategory", "network") // не совпадает
            ));
            assertFalse(condition.evaluate(INCIDENT_CRITICAL));
        }
    }

    @Nested
    @DisplayName("OrCondition")
    class OrConditionTests {

        @Test
        @DisplayName("Хотя бы одно истинно → true")
        void oneTrue() {
            var condition = new OrCondition(List.of(
                new SimpleCondition("ticketType", "service_request"),
                new SimpleCondition("ticketType", "incident")
            ));
            assertTrue(condition.evaluate(INCIDENT_CRITICAL));
        }

        @Test
        @DisplayName("Все ложны → false")
        void allFalse() {
            var condition = new OrCondition(List.of(
                new SimpleCondition("ticketType", "problem"),
                new SimpleCondition("ticketType", "change")
            ));
            assertFalse(condition.evaluate(INCIDENT_CRITICAL));
        }
    }

    @Nested
    @DisplayName("NotCondition")
    class NotConditionTests {

        @Test
        @DisplayName("Отрицание true → false")
        void negateTrue() {
            assertFalse(new NotCondition(new SimpleCondition("ticketType", "incident"))
                .evaluate(INCIDENT_CRITICAL));
        }

        @Test
        @DisplayName("Отрицание false → true")
        void negateFalse() {
            assertTrue(new NotCondition(new SimpleCondition("ticketType", "problem"))
                .evaluate(INCIDENT_CRITICAL));
        }
    }
}
