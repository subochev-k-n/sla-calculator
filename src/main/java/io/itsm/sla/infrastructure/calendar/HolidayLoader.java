package io.itsm.sla.infrastructure.calendar;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.itsm.sla.calendar.BusinessDayCalendar;
import io.itsm.sla.calendar.Holiday;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/**
 * Загрузчик производственного календаря из YAML-файла.
 */
@Slf4j
@Component
public class HolidayLoader {

    private final ObjectMapper yamlObjectMapper;
    private final ResourceLoader resourceLoader;
    private final BusinessDayCalendar calendar;

    public HolidayLoader(@Qualifier("yamlObjectMapper") ObjectMapper yamlObjectMapper,
                         ResourceLoader resourceLoader,
                         BusinessDayCalendar calendar) {
        this.yamlObjectMapper = yamlObjectMapper;
        this.resourceLoader = resourceLoader;
        this.calendar = calendar;
    }

    @PostConstruct
    public void loadHolidays() {
        try {
            var resource = resourceLoader.getResource("classpath:holidays.yaml");
            if (!resource.exists()) {
                log.warn("Holidays YAML not found, using empty calendar");
                return;
            }
            try (InputStream is = resource.getInputStream()) {
                var config = yamlObjectMapper.readValue(is, HolidaysYaml.class);
                var holidays = config.toDomain();
                calendar.initHolidays(holidays);
                log.info("Loaded {} holidays from holidays.yaml", holidays.size());
            }
        } catch (Exception e) {
            log.error("Failed to load holidays", e);
        }
    }

    public record HolidaysYaml(List<HolidayYaml> holidays) {
        public List<Holiday> toDomain() {
            return holidays.stream()
                .map(HolidayYaml::toDomain)
                .toList();
        }
    }

    public record HolidayYaml(
        String date,
        String name,
        String type,
        String zone
    ) {
        public Holiday toDomain() {
            return new Holiday(
                LocalDate.parse(date),
                name,
                Holiday.HolidayType.valueOf(type.toUpperCase()),
                zone != null ? ZoneId.of(zone) : ZoneId.of("Europe/Moscow")
            );
        }
    }
}
