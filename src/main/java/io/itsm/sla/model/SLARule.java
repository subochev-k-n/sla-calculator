package io.itsm.sla.model;

import io.itsm.sla.model.action.SLAAction;
import io.itsm.sla.model.condition.Condition;

/**
 * Правило SLA — связывает условие с действием.
 * <p>
 * Каждое правило имеет приоритет: правила с меньшим значением проверяются раньше.
 * Первое подошедшее правило применяется (first-match).
 */
public record SLARule(
    String id,
    String name,
    String description,
    int priority,
    Condition condition,
    SLAAction action,
    boolean active
) {

    public boolean matches(SLAContext context) {
        return active && condition.evaluate(context);
    }
}
