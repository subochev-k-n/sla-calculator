package io.itsm.sla.web;

import io.itsm.sla.model.SLAContext;
import io.itsm.sla.model.SLADeadline;
import io.itsm.sla.service.DeadlineCalculator;
import io.itsm.sla.web.dto.SLAScenarioDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * REST-контроллер предустановленных сценариев SLA для веб-интерфейса.
 * <p>
 * Веб-интерфейс (index.html) — статический файл, обслуживается из
 * {@code src/main/resources/static/index.html} по корневому пути.
 */
@RestController
@RequiredArgsConstructor
public class SLAWebController {

    private final DeadlineCalculator calculator;

    /**
     * Карта предустановленных сценариев SLA для веб-интерфейса.
     */
    private static final Map<String, SLAScenarioDTO> SCENARIOS = new LinkedHashMap<>();

    static {
        Stream.of(
            SLAScenarioDTO.of("incident-critical",   "Критичный инцидент (P1)",        "4 часа, 24/7, с учётом праздников",          "incident",        "critical", "", "PT4H"),
            SLAScenarioDTO.of("incident-high",       "Высокий инцидент (P2)",          "8 рабочих часов, Пн-Пт 9:00–18:00",          "incident",        "high",     "", "PT8H"),
            SLAScenarioDTO.of("incident-medium",     "Средний инцидент (P3)",          "24 рабочих часа, Пн-Пт 9:00–18:00",          "incident",        "medium",   "", "PT24H"),
            SLAScenarioDTO.of("incident-low",        "Низкий инцидент (P4)",           "5 рабочих дней, Пн-Пт 9:00–18:00",           "incident",        "low",      "", "P5D"),
            SLAScenarioDTO.of("service-request-sw",  "Запрос на ПО",                   "3 рабочих дня",                               "service_request", "", "software", "P3D"),
            SLAScenarioDTO.of("service-request-hw",  "Запрос на оборудование",         "10 рабочих дней",                              "service_request", "", "hardware", "P10D"),
            SLAScenarioDTO.of("change-standard",     "Стандартное изменение",          "5 рабочих дней, риск ≤ средний",              "change",          "", "", "P5D"),
            SLAScenarioDTO.of("maintenance-monthly", "Регламентное ТО (ежемесячное)",  "Последнее воскресенье месяца, 02:00",         "maintenance",     "", "", "—"),
            SLAScenarioDTO.of("outage-recovery",     "Восстановление после сбоя",      "30 минут, 24/7",                               "outage",          "", "", "PT30M")
        ).forEach(s -> SCENARIOS.put(s.id(), s));
    }

    /**
     * GET /api/v1/sla/scenarios — список предустановленных сценариев.
     */
    @GetMapping(value = "/api/v1/sla/scenarios", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<SLAScenarioDTO>> listScenarios() {
        return ResponseEntity.ok(List.copyOf(SCENARIOS.values()));
    }

    /**
     * POST /api/v1/sla/scenario/calculate — расчёт дедлайна по сценарию.
     * <p>
     * Принимает JSON: { "scenarioId": "...", "startTime": "2026-06-21T14:00:00+03:00" }
     */
    @PostMapping(value = "/api/v1/sla/scenario/calculate",
                 consumes = MediaType.APPLICATION_JSON_VALUE,
                 produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> calculateScenario(
            @RequestBody Map<String, String> request) {

        var scenarioId = request.get("scenarioId");
        var scenario = SCENARIOS.get(scenarioId);
        if (scenario == null) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Неизвестный сценарий: " + scenarioId
            ));
        }

        var startTimeStr = request.get("startTime");
        var startTime = startTimeStr != null && !startTimeStr.isBlank()
            ? ZonedDateTime.parse(startTimeStr)
            : ZonedDateTime.now();

        var attrs = new java.util.HashMap<String, String>();
        if (scenario.urgency() != null && !scenario.urgency().isBlank()) {
            attrs.put("urgency", scenario.urgency());
        }
        if (scenario.category() != null && !scenario.category().isBlank()) {
            attrs.put("category", scenario.category());
        }

        var ctx = SLAContext.builder()
            .ticketType(scenario.ticketType())
            .attributes(attrs)
            .build();

        SLADeadline deadline;
        try {
            deadline = calculator.computeDeadline(ctx, startTime);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Ошибка расчёта: " + e.getMessage()
            ));
        }

        var fmt = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm (VV)");
        var result = new LinkedHashMap<String, Object>();
        result.put("scenarioId", scenario.id());
        result.put("scenarioName", scenario.name());
        result.put("startTime", startTime.format(fmt));
        result.put("deadline", deadline.deadline().format(fmt));
        result.put("deadlineIso", deadline.deadline().toString());
        result.put("slaRuleId", deadline.slaRuleId());
        result.put("slaRuleName", deadline.slaRuleName());
        result.put("durationMinutes", deadline.durationMinutes());
        result.put("isOverdue", deadline.isOverdue(ZonedDateTime.now()));
        result.put("description", scenario.description());
        return ResponseEntity.ok(result);
    }
}
