package io.itsm.sla.model.action;

import java.time.Duration;

/**
 * Действие: эскалация — через заданное время применяется другой SLA.
 * <p>
 * Позволяет моделировать каскадные SLA:
 * - P1: 15 минут → если не решено → P2: 4 часа → если не решено → P3: 24 часа
 */
public record EscalateAction(
    Duration initialDuration,
    SLAAction escalateTo
) implements SLAAction {

    public EscalateAction {
        if (initialDuration == null || initialDuration.isNegative()) {
            throw new IllegalArgumentException("initialDuration must be positive");
        }
        if (escalateTo == null) {
            throw new IllegalArgumentException("escalateTo must not be null");
        }
    }

    /**
     * Полная длительность с учётом эскалации (рекурсивно).
     */
    public Duration totalDuration() {
        Duration total = initialDuration;
        if (escalateTo instanceof DurationAction da) {
            total = total.plus(da.duration());
        } else if (escalateTo instanceof EscalateAction ea) {
            total = total.plus(ea.totalDuration());
        }
        return total;
    }
}
