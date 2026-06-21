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

    private int compareNumeric(String a, String b) {
        try {
            return Long.compare(Long.parseLong(a), Long.parseLong(b));
        } catch (NumberFormatException e) {
            return a.compareTo(b);
        }
    }
}
