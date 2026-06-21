package io.itsm.sla.model.condition;

import io.itsm.sla.model.SLAContext;
import java.util.List;

/**
 * AND-условие: все дочерние условия должны быть истинны.
 */
public record AndCondition(List<Condition> conditions) implements Condition {
    @Override
    public boolean evaluate(SLAContext context) {
        return conditions.stream().allMatch(c -> c.evaluate(context));
    }
}
