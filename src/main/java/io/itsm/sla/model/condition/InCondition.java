package io.itsm.sla.model.condition;

import io.itsm.sla.model.SLAContext;
import java.util.Set;

/**
 * Условие: значение поля context входит в множество.
 */
public record InCondition(String field, Set<String> values) implements Condition {

    @Override
    public boolean evaluate(SLAContext context) {
        var actual = resolveField(context);
        return actual != null && values.contains(actual);
    }

    private String resolveField(SLAContext ctx) {
        return switch (field) {
            case "ticketType" -> ctx.ticketType();
            case "ticketCategory" -> ctx.ticketCategory();
            case "ticketSubcategory" -> ctx.ticketSubcategory();
            default -> null;
        };
    }
}
