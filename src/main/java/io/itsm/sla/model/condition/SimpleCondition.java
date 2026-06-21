package io.itsm.sla.model.condition;

import io.itsm.sla.model.SLAContext;

/**
 * Простое условие: значение поля SLAContext совпадает с ожидаемым.
 * <p>
 * Пример: ticketType == "incident"
 */
public record SimpleCondition(String field, String expectedValue) implements Condition {
    @Override
    public boolean evaluate(SLAContext context) {
        var actual = resolveField(context);
        return expectedValue.equals(actual);
    }

    private String resolveField(SLAContext ctx) {
        return switch (field) {
            case "ticketType" -> ctx.ticketType();
            case "ticketCategory" -> ctx.ticketCategory();
            case "ticketSubcategory" -> ctx.ticketSubcategory();
            case "ticketType.category" -> ctx.ticketType() + "." + ctx.ticketCategory();
            default -> null;
        };
    }
}
