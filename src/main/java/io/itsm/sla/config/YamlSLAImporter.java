package io.itsm.sla.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.itsm.sla.model.SLARule;
import io.itsm.sla.model.action.DurationAction;
import io.itsm.sla.model.action.FixedDeadlineAction;
import io.itsm.sla.model.action.SLAAction;
import io.itsm.sla.model.condition.*;
import io.itsm.sla.model.TimeWindow;
import io.itsm.sla.port.SLARuleLoader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Загрузчик SLA-правил из YAML-файла.
 */
@Slf4j
@Component
public class YamlSLAImporter {

    private final ObjectMapper yamlObjectMapper;
    private final ResourceLoader resourceLoader;
    private final SLARuleLoader ruleLoader;

    public YamlSLAImporter(@Qualifier("yamlObjectMapper") ObjectMapper yamlObjectMapper,
                           ResourceLoader resourceLoader,
                           SLARuleLoader ruleLoader) {
        this.yamlObjectMapper = yamlObjectMapper;
        this.resourceLoader = resourceLoader;
        this.ruleLoader = ruleLoader;
    }

    @SuppressWarnings("unchecked")
    public void loadFromYaml(String yamlPath) {
        try {
            var resource = resourceLoader.getResource(yamlPath);
            if (!resource.exists()) {
                log.warn("SLA rules YAML not found: {}", yamlPath);
                return;
            }
            try (InputStream is = resource.getInputStream()) {
                var raw = yamlObjectMapper.readValue(is, Map.class);
                var rulesRaw = (List<Map<String, Object>>) raw.get("rules");
                var rules = new ArrayList<SLARule>();

                for (var r : rulesRaw) {
                    var id = (String) r.get("id");
                    var name = (String) r.getOrDefault("name", id);
                    var desc = (String) r.getOrDefault("description", "");
                    var priority = (int) r.getOrDefault("priority", 999);
                    var active = (boolean) r.getOrDefault("active", true);

                    // Парсим condition
                    var conditionRaw = (Map<String, Object>) r.get("condition");
                    var condition = parseCondition(conditionRaw);

                    // Парсим action
                    var actionRaw = (Map<String, Object>) r.get("action");
                    var action = parseAction(actionRaw);

                    rules.add(new SLARule(id, name, desc, priority, condition, action, active));
                }

                ruleLoader.saveAll(rules);
                log.info("Loaded {} SLA rules from {}", rules.size(), yamlPath);
            }
        } catch (Exception e) {
            log.error("Failed to load SLA rules from {}", yamlPath, e);
        }
    }

    @SuppressWarnings("unchecked")
    private Condition parseCondition(Map<String, Object> raw) {
        if (raw == null || raw.isEmpty()) return new TrueCondition();
        var typeObj = raw.get("type");
        if (typeObj == null) {
            // Auto-detect: если есть field → simple, если key → attribute
            if (raw.containsKey("field")) {
                return new SimpleCondition((String) raw.get("field"), (String) raw.get("value"));
            }
            if (raw.containsKey("key")) {
                return parseAttributeCondition(raw);
            }
            return new TrueCondition();
        }
        var type = typeObj.toString();

        return switch (type) {
            case "simple", "eq" -> {
                var field = (String) raw.get("field");
                var value = (String) raw.get("value");
                yield new SimpleCondition(field, value);
            }
            case "attribute" -> parseAttributeCondition(raw);
            case "and" -> {
                var conditions = ((List<Map<String, Object>>) raw.get("conditions"))
                    .stream().map(this::parseCondition).toList();
                yield new AndCondition(conditions);
            }
            case "or" -> {
                var conditions = ((List<Map<String, Object>>) raw.get("conditions"))
                    .stream().map(this::parseCondition).toList();
                yield new OrCondition(conditions);
            }
            case "not" -> new NotCondition(parseCondition((Map<String, Object>) raw.get("condition")));
            case "in" -> {
                var field = (String) raw.get("field");
                var values = raw.get("values");
                Set<String> valSet;
                if (values instanceof List) {
                    valSet = new HashSet<>((List<String>) values);
                } else {
                    valSet = Set.of(((String) values).split(","));
                }
                yield new InCondition(field, valSet);
            }
            case "true" -> new TrueCondition();
            default -> {
                log.warn("Unknown condition type: {}", type);
                yield new TrueCondition();
            }
        };
    }

    @SuppressWarnings("unchecked")
    private SLAAction parseAction(Map<String, Object> raw) {
        if (raw == null) {
            return new DurationAction(Duration.ofHours(24), List.of(), "HOURS");
        }
        var type = (String) raw.get("type");

        return switch (type) {
            case "duration" -> {
                var duration = Duration.parse((String) raw.get("duration"));
                var windowsRaw = (List<Map<String, Object>>) raw.get("timeWindows");
                var windows = windowsRaw != null
                    ? windowsRaw.stream().map(this::parseTimeWindow).toList()
                    : List.<TimeWindow>of();
                var precision = raw.get("precision") != null
                    ? (String) raw.get("precision")
                    : detectPrecision((String) raw.get("duration"));
                yield new DurationAction(duration, windows, precision);
            }
            case "fixed-deadline" -> {
                var expr = (String) raw.get("expression");
                var zoneStr = (String) raw.getOrDefault("zone", "UTC");
                yield new FixedDeadlineAction(expr, ZoneId.of(zoneStr));
            }
            default -> {
                log.warn("Unknown action type: {}, using default 24h", type);
                yield new DurationAction(Duration.ofHours(24), List.of(), "HOURS");
            }
        };
    }

    private AttributeCondition parseAttributeCondition(Map<String, Object> raw) {
        var key = (String) raw.get("key");
        var opStr = ((String) raw.getOrDefault("operator", "eq")).toUpperCase();
        var value = raw.get("value") != null ? raw.get("value").toString() : "";
        var op = AttributeCondition.Operator.valueOf(opStr);
        return new AttributeCondition(key, op, value);
    }

    private String detectPrecision(String durationStr) {
        if (durationStr == null) return "MINUTES";
        if (durationStr.matches("P\\d+D")) return "DAYS";
        if (durationStr.matches("PT\\d+H")) return "HOURS";
        return "MINUTES";
    }

    private TimeWindow parseTimeWindow(Map<String, Object> raw) {
        var name = (String) raw.getOrDefault("name", "window");
        var daysRaw = (List<String>) raw.get("weekDays");
        Set<DayOfWeek> weekDays;
        if (daysRaw != null) {
            weekDays = daysRaw.stream()
                .map(d -> {
                    if (d.contains("..")) {
                        var parts = d.split("\\.\\.");
                        var start = DayOfWeek.valueOf(parts[0]);
                        var end = DayOfWeek.valueOf(parts[1]);
                        var days = new ArrayList<DayOfWeek>();
                        for (int i = start.getValue(); i <= end.getValue(); i++) {
                            days.add(DayOfWeek.of(i));
                        }
                        return days;
                    }
                    return List.of(DayOfWeek.valueOf(d));
                })
                .flatMap(List::stream)
                .collect(Collectors.toSet());
        } else {
            weekDays = Set.of(DayOfWeek.values());
        }

        var dayStart = raw.get("dayStart") != null
            ? LocalTime.parse((String) raw.get("dayStart"))
            : LocalTime.MIN;
        var dayEnd = raw.get("dayEnd") != null
            ? LocalTime.parse((String) raw.get("dayEnd"))
            : LocalTime.MAX;
        var zone = raw.get("zone") != null
            ? ZoneId.of((String) raw.get("zone"))
            : ZoneId.of("UTC");
        var includeHolidays = (boolean) raw.getOrDefault("includeHolidays", true);

        return new TimeWindow(name, weekDays, dayStart, dayEnd, zone, includeHolidays, null, null);
    }
}
