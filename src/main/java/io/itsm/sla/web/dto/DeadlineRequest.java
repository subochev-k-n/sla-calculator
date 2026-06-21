package io.itsm.sla.web.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.ZonedDateTime;
import java.util.Map;

/**
 * Запрос на вычисление SLA-дедлайна.
 */
public record DeadlineRequest(
    @NotBlank String ticketType,
    String ticketCategory,
    String ticketSubcategory,
    ZonedDateTime startTime,
    Map<String, String> attributes
) {
    public DeadlineRequest {
        if (attributes == null) attributes = Map.of();
    }
}
