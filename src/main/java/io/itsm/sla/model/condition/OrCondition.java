package io.itsm.sla.model.condition;

import io.itsm.sla.model.SLAContext;
import java.util.List;

/**
 * OR-условие: хотя бы одно дочернее условие истинно.
 */
public record OrCondition(List<Condition> conditions) implements Condition {
    @Override
    public boolean evaluate(SLAContext context) {
        return conditions.stream().anyMatch(c -> c.evaluate(context));
    }
}
