package io.itsm.sla.model.condition;

import io.itsm.sla.model.SLAContext;
import java.util.Arrays;

/**
 * Условие на произвольный атрибут SLAContext.attributes.
 * Поддерживает операторы: eq, neq, gte, lte, gt, lt.
 */
public record AttributeCondition(String key, Operator operator, String value) implements Condition {

    public enum Operator {
        EQ, NEQ, GTE, LTE, GT, LT, EXISTS, IN
    }

    @Override
    public boolean evaluate(SLAContext context) {
        var actual = context.getAttribute(key);
        if (actual == null) {
            return false; // атрибут отсутствует — ни одно условие не выполнено
        }

        return switch (operator) {
            case EQ -> actual.equals(value);
            case NEQ -> !actual.equals(value);
            case GTE -> compareNumeric(actual, value) >= 0;
            case LTE -> compareNumeric(actual, value) <= 0;
            case GT -> compareNumeric(actual, value) > 0;
            case LT -> compareNumeric(actual, value) < 0;
            case EXISTS -> true;
            case IN -> Arrays.stream(value.split(",")).anyMatch(v -> actual.trim().equals(v.trim()));
        };
    }

    /**
     * Порядок уровней urgency для строкового сравнения.
     */
    private static final java.util.List<String> URGENCY_ORDER = java.util.List.of(
        "critical", "high", "medium", "low"
    );

    private int compareNumeric(String a, String b) {
        try {
            return Long.compare(Long.parseLong(a), Long.parseLong(b));
        } catch (NumberFormatException e) {
            // Пробуем сравнить как urgency
            int ia = URGENCY_ORDER.indexOf(a.toLowerCase());
            int ib = URGENCY_ORDER.indexOf(b.toLowerCase());
            if (ia >= 0 && ib >= 0) {
                // Инвертируем: чем меньше индекс, тем выше приоритет
                return Integer.compare(ib, ia);
            }
            return a.compareToIgnoreCase(b);
        }
    }
}
