package io.itsm.sla.port;

import io.itsm.sla.model.SLARule;

import java.util.List;

/**
 * Порт для сохранения SLA-правил (из YAML-importer в БД).
 */
public interface SLARuleLoader {

    void saveAll(List<SLARule> rules);

    List<SLARule> loadAll();
}
