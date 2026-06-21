package io.itsm.sla.model.action;

import java.time.Month;
import java.time.ZoneId;

/**
 * Действие: дедлайн = фиксированное время, заданное выражением.
 * <p>
 * Примеры выражений:
 * - "last-business-day-of-month 18:00" — последний рабочий день месяца в 18:00
 * - "next-monday 09:00" — следующий понедельник 09:00
 * - "2026-12-31T23:59" — абсолютная дата
 * - "end-of-next-business-day" — конец следующего рабочего дня
 */
public record FixedDeadlineAction(
    String expression,
    ZoneId zone
) implements SLAAction {

    public FixedDeadlineAction {
        if (expression == null || expression.isBlank()) {
            throw new IllegalArgumentException("expression must not be blank");
        }
    }

    /**
     * Тип выражения (определяется на этапе конфигурации).
     */
    public ExpressionType detectType() {
        if (expression.contains("last-business-day")) return ExpressionType.LAST_BUSINESS_DAY;
        if (expression.contains("next-monday") || expression.contains("next-")) return ExpressionType.NEXT_WEEKDAY;
        if (expression.contains("end-of-next-business-day")) return ExpressionType.END_OF_NEXT_DAY;
        if (expression.contains("T") || expression.matches("\\d{4}-\\d{2}-\\d{2}.*")) return ExpressionType.ABSOLUTE;
        if (expression.matches("P.*")) return ExpressionType.DURATION;
        return ExpressionType.CUSTOM;
    }

    public enum ExpressionType {
        LAST_BUSINESS_DAY,
        NEXT_WEEKDAY,
        END_OF_NEXT_DAY,
        ABSOLUTE,
        DURATION,
        CUSTOM
    }
}
