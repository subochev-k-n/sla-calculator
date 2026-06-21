package io.itsm.sla.model;

import java.time.ZonedDateTime;

/**
 * Результат расчёта SLA-дедлайна.
 */
public record SLADeadline(
    ZonedDateTime deadline,
    String slaRuleId,
    String slaRuleName,
    long durationMinutes,
    boolean compliant,
    String description
) {
    public SLADeadline {
        if (deadline == null) throw new IllegalArgumentException("deadline must not be null");
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private ZonedDateTime deadline;
        private String slaRuleId;
        private String slaRuleName;
        private long durationMinutes;
        private boolean compliant;
        private String description;

        public Builder deadline(ZonedDateTime deadline) { this.deadline = deadline; return this; }
        public Builder slaRuleId(String slaRuleId) { this.slaRuleId = slaRuleId; return this; }
        public Builder slaRuleName(String slaRuleName) { this.slaRuleName = slaRuleName; return this; }
        public Builder durationMinutes(long durationMinutes) { this.durationMinutes = durationMinutes; return this; }
        public Builder compliant(boolean compliant) { this.compliant = compliant; return this; }
        public Builder description(String description) { this.description = description; return this; }

        public SLADeadline build() {
            return new SLADeadline(deadline, slaRuleId, slaRuleName, durationMinutes, compliant, description);
        }
    }

    public boolean isOverdue(ZonedDateTime now) {
        return now.isAfter(deadline);
    }
}
