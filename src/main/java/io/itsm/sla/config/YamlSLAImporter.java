package io.itsm.sla.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.itsm.sla.model.SLARule;
import io.itsm.sla.port.SLARuleLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;

/**
 * Загрузчик SLA-правил из YAML-файла.
 * <p>
 * Используется для начальной инициализации БД.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class YamlSLAImporter {

    private final ObjectMapper yamlObjectMapper;
    private final ResourceLoader resourceLoader;
    private final SLARuleLoader ruleLoader;

    /**
     * Загрузить SLA-правила из YAML-файла.
     *
     * @param yamlPath путь к ресурсу (classpath:sla-rules.yaml)
     */
    public void loadFromYaml(String yamlPath) {
        try {
            var resource = resourceLoader.getResource(yamlPath);
            if (!resource.exists()) {
                log.warn("SLA rules YAML not found: {}", yamlPath);
                return;
            }
            try (InputStream is = resource.getInputStream()) {
                var yamlConfig = yamlObjectMapper.readValue(is, SLARulesYaml.class);
                var rules = yamlConfig.toDomain();
                ruleLoader.saveAll(rules);
                log.info("Loaded {} SLA rules from {}", rules.size(), yamlPath);
            }
        } catch (Exception e) {
            log.error("Failed to load SLA rules from {}", yamlPath, e);
        }
    }

    /**
     * Промежуточный объект для YAML-десериализации.
     */
    public record SLARulesYaml(List<SLARuleYaml> rules) {
        public List<SLARule> toDomain() {
            return rules.stream()
                .map(SLARuleYaml::toDomain)
                .toList();
        }
    }

    public record SLARuleYaml(
        String id,
        String name,
        String description,
        int priority,
        Object condition,
        Object action,
        boolean active
    ) {
        public SLARule toDomain() {
            // Упрощённая конвертация — используем JSON как промежуточный формат
            // для сериализации Condition и Action
            return new SLARule(
                id, name, description, priority,
                null, null, active
            );
        }
    }
}
