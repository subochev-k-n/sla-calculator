package io.itsm.sla.model.action;

/**
 * Базовый интерфейс действия SLA — определяет, КАК вычислять дедлайн.
 * <p>
 * Каждый тип действия реализует свою стратегию расчёта.
 */
public sealed interface SLAAction permits DurationAction, FixedDeadlineAction,
    EscalateAction, CompositeAction {
}
