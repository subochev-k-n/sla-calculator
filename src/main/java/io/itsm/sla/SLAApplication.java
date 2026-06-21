package io.itsm.sla;

import io.itsm.sla.calendar.BusinessDayCalendar;
import io.itsm.sla.config.YamlSLAImporter;
import io.itsm.sla.port.SLARuleLoader;
import io.itsm.sla.service.DeadlineCalculator;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.List;

@Slf4j
@EnableCaching
@SpringBootApplication
public class SLAApplication {

    private final YamlSLAImporter yamlImporter;

    public SLAApplication(YamlSLAImporter yamlImporter) {
        this.yamlImporter = yamlImporter;
    }

    public static void main(String[] args) {
        SpringApplication.run(SLAApplication.class, args);
    }

    @PostConstruct
    void init() {
        log.info("Initializing SLA Calculator Service...");
        yamlImporter.loadFromYaml("classpath:sla-rules.yaml");
        log.info("SLA Calculator Service initialized");
    }

    @Bean
    public BusinessDayCalendar businessDayCalendar() {
        return new BusinessDayCalendar();
    }

    @Bean
    public DeadlineCalculator deadlineCalculator(
            SLARuleLoader ruleLoader,
            BusinessDayCalendar calendar) {
        return new DeadlineCalculator(ruleLoader, calendar);
    }
}
