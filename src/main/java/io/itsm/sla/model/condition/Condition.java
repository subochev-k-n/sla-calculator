package io.itsm.sla.model.condition;

import io.itsm.sla.model.SLAContext;

/**
 * Базовый интерфейс условия для выбора SLA-правила.
 * Поддерживает композицию (AND, OR, NOT) и простые сравнения.
 */
public sealed interface Condition permits SimpleCondition, AttributeCondition,
    InCondition, AndCondition, OrCondition, NotCondition, TrueCondition {

    boolean evaluate(SLAContext context);
}
