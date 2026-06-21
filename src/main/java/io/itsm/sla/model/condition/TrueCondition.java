package io.itsm.sla.model.condition;

import io.itsm.sla.model.SLAContext;

/**
 * Безусловно истинное условие — используется как fallback/default-правило.
 */
public record TrueCondition() implements Condition {
    @Override
    public boolean evaluate(SLAContext context) {
        return true;
    }
}
