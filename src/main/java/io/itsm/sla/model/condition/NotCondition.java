package io.itsm.sla.model.condition;

import io.itsm.sla.model.SLAContext;

/**
 * NOT-условие: отрицание дочернего условия.
 */
public record NotCondition(Condition condition) implements Condition {
    @Override
    public boolean evaluate(SLAContext context) {
        return !condition.evaluate(context);
    }
}
