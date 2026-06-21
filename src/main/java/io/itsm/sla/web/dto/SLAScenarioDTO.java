package io.itsm.sla.web.dto;

/**
 * DTO предустановленного SLA-сценария для веб-интерфейса.
 */
public record SLAScenarioDTO(
    String id,
    String name,
    String description,
    String ticketType,
    String urgency,
    String category,
    String duration
) {
    public static SLAScenarioDTO of(String id, String name, String description,
                                     String ticketType, String urgency,
                                     String category, String duration) {
        return new SLAScenarioDTO(id, name, description, ticketType, urgency, category, duration);
    }
}
