package io.itsm.sla.model;

import java.time.ZonedDateTime;

/**
 * Результат расчёта SLA-дедлайна.
 */
public record SLADeadline(
    ZonedDateTime deadline,
    ZonedDateTime effectiveStartTime,
    String slaRuleId,
    String slaRuleName,
    long durationMinutes,
    boolean compliant,
    String description
) {
    public SLADeadline {
        if (deadline == null) throw new IllegalArgumentException("deadline must not be null");
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private ZonedDateTime deadline;
        private ZonedDateTime effectiveStartTime;
        private String slaRuleId, slaRuleName, description;
        private long durationMinutes;
        private boolean compliant;

        public Builder deadline(ZonedDateTime v) { this.deadline = v; return this; }
        public Builder effectiveStartTime(ZonedDateTime v) { this.effectiveStartTime = v; return this; }
        public Builder slaRuleId(String v) { this.slaRuleId = v; return this; }
        public Builder slaRuleName(String v) { this.slaRuleName = v; return this; }
        public Builder durationMinutes(long v) { this.durationMinutes = v; return this; }
        public Builder compliant(boolean v) { this.compliant = v; return this; }
        public Builder description(String v) { this.description = v; return this; }

        public SLADeadline build() {
            return new SLADeadline(deadline, effectiveStartTime, slaRuleId, slaRuleName, durationMinutes, compliant, description);
        }
    }

    public boolean isOverdue(ZonedDateTime now) { return now.isAfter(deadline); }
}
