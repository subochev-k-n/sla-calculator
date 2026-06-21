package io.itsm.sla.model;

import java.util.Map;

/**
 * Контекст обращения — все входные данные для выбора SLA-правила и расчёта дедлайна.
 * <p>
 * Содержит классификацию обращения, атрибуты и временные параметры.
 * Расширяется через {@code attributes} без изменения схемы.
 */
public record SLAContext(
    String ticketType,
    String ticketCategory,
    String ticketSubcategory,
    Map<String, String> attributes
) {
    public SLAContext {
        attributes = (attributes != null) ? Map.copyOf(attributes) : Map.of();
    }

    /**
     * Получить значение атрибута с дефолтом.
     */
    public String getAttribute(String key) {
        return attributes.get(key);
    }

    /**
     * Получить значение атрибута с дефолтным значением.
     */
    public String getAttribute(String key, String defaultValue) {
        return attributes.getOrDefault(key, defaultValue);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String ticketType;
        private String ticketCategory;
        private String ticketSubcategory;
        private Map<String, String> attributes;

        public Builder ticketType(String ticketType) { this.ticketType = ticketType; return this; }
        public Builder ticketCategory(String ticketCategory) { this.ticketCategory = ticketCategory; return this; }
        public Builder ticketSubcategory(String ticketSubcategory) { this.ticketSubcategory = ticketSubcategory; return this; }
        public Builder attributes(Map<String, String> attributes) { this.attributes = attributes; return this; }

        public SLAContext build() {
            return new SLAContext(ticketType, ticketCategory, ticketSubcategory, attributes);
        }
    }
}
