package io.itsm.sla.web;

import io.itsm.sla.model.SLAContext;
import io.itsm.sla.model.SLADeadline;
import io.itsm.sla.service.DeadlineCalculator;
import io.itsm.sla.web.dto.DeadlineRequest;
import io.itsm.sla.web.dto.DeadlineResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.ZonedDateTime;
import java.util.Map;

/**
 * REST-контроллер для расчёта SLA-дедлайнов.
 */
@RestController
@RequestMapping("/api/v1/sla")
@RequiredArgsConstructor
public class SLAComputeController {

    private final DeadlineCalculator calculator;

    /**
     * Вычислить дедлайн для обращения.
     *
     * POST /api/v1/sla/compute
     */
    @PostMapping("/compute")
    public ResponseEntity<DeadlineResponse> computeDeadline(
            @Valid @RequestBody DeadlineRequest request) {

        var startTime = request.startTime() != null
            ? request.startTime()
            : ZonedDateTime.now();

        var context = SLAContext.builder()
            .ticketType(request.ticketType())
            .ticketCategory(request.ticketCategory())
            .ticketSubcategory(request.ticketSubcategory())
            .attributes(request.attributes())
            .build();

        var deadline = calculator.computeDeadline(context, startTime);

        return ResponseEntity.ok(DeadlineResponse.from(deadline));
    }

    /**
     * Health-check.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "sla-calculator"));
    }
}
