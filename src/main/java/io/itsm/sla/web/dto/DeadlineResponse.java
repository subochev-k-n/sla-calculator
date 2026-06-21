package io.itsm.sla.web.dto;

import io.itsm.sla.model.SLADeadline;

import java.time.ZonedDateTime;

/**
 * Ответ с результатом расчёта SLA-дедлайна.
 */
public record DeadlineResponse(
    ZonedDateTime deadline,
    String slaRuleId,
    String slaRuleName,
    long durationMinutes,
    boolean compliant,
    boolean isOverdue,
    ZonedDateTime calculatedAt
) {
    public static DeadlineResponse from(SLADeadline deadline) {
        return new DeadlineResponse(
            deadline.deadline(),
            deadline.slaRuleId(),
            deadline.slaRuleName(),
            deadline.durationMinutes(),
            deadline.compliant(),
            deadline.isOverdue(ZonedDateTime.now()),
            ZonedDateTime.now()
        );
    }
}
