package io.itsm.sla.model.action;

import java.util.List;

/**
 * Композитное действие: список действий, применяемых последовательно.
 * <p>
 * Позволяет комбинировать разные типы SLA в одном правиле.
 */
public record CompositeAction(
    List<SLAAction> actions
) implements SLAAction {

    public CompositeAction {
        if (actions == null || actions.isEmpty()) {
            throw new IllegalArgumentException("actions must not be empty");
        }
    }

    public SLAAction first() {
        return actions.getFirst();
    }
}
