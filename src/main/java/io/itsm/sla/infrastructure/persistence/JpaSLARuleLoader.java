package io.itsm.sla.infrastructure.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.itsm.sla.model.SLARule;
import io.itsm.sla.model.action.*;
import io.itsm.sla.model.condition.*;
import io.itsm.sla.port.SLARuleLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Адаптер JPA для загрузки/сохранения SLA-правил.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JpaSLARuleLoader implements SLARuleLoader {

    private final SLARuleRepository repository;
    private final ObjectMapper jsonMapper;

    @Override
    public void saveAll(List<SLARule> rules) {
        var entities = rules.stream()
            .map(this::toEntity)
            .toList();
        repository.saveAll(entities);
    }

    @Override
    public List<SLARule> loadAll() {
        return repository.findByActiveTrueOrderByPriorityAsc()
            .stream()
            .map(this::toDomain)
            .toList();
    }

    private SLARuleEntity toEntity(SLARule rule) {
        var entity = new SLARuleEntity();
        entity.setId(rule.id());
        entity.setPriority(rule.priority());
        entity.setActive(rule.active());
        try {
            entity.setConditionJson(jsonMapper.writeValueAsString(rule.condition()));
            entity.setActionJson(jsonMapper.writeValueAsString(rule.action()));
        } catch (Exception e) {
            log.error("Failed to serialize SLA rule {}", rule.id(), e);
            throw new RuntimeException(e);
        }
        return entity;
    }

    private SLARule toDomain(SLARuleEntity entity) {
        try {
            var condition = jsonMapper.readValue(entity.getConditionJson(), Condition.class);
            var action = jsonMapper.readValue(entity.getActionJson(), SLAAction.class);
            return new SLARule(
                entity.getId(),
                entity.getId(),  // name == id как fallback
                null,
                entity.getPriority(),
                condition,
                action,
                entity.isActive()
            );
        } catch (Exception e) {
            log.error("Failed to deserialize SLA rule {}", entity.getId(), e);
            throw new RuntimeException(e);
        }
    }
}
