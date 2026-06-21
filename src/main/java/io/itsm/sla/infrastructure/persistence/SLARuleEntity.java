package io.itsm.sla.infrastructure.persistence;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * JPA-сущность для хранения SLA-правил.
 */
@Entity
@Table(name = "sla_rule")
public class SLARuleEntity {

    @Id
    @Column(length = 64)
    private String id;

    @Column(nullable = false)
    private int priority;

    @Column(name = "condition_json", nullable = false, columnDefinition = "CLOB")
    private String conditionJson;

    @Column(name = "action_json", nullable = false, columnDefinition = "CLOB")
    private String actionJson;

    @Column(nullable = false)
    private boolean active = true;

    @Version
    private long version = 1;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // --- Getters / Setters (Lombok @Data не используем для JPA-сущностей) ---

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }

    public String getConditionJson() { return conditionJson; }
    public void setConditionJson(String conditionJson) { this.conditionJson = conditionJson; }

    public String getActionJson() { return actionJson; }
    public void setActionJson(String actionJson) { this.actionJson = actionJson; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public long getVersion() { return version; }
    public void setVersion(long version) { this.version = version; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
