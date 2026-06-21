package io.itsm.sla.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.itsm.sla.infrastructure.persistence.JacksonActionModule;
import io.itsm.sla.infrastructure.persistence.JacksonConditionModule;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Основная конфигурация приложения.
 */
@Configuration
public class SLAConfig {

    /**
     * ObjectMapper, поддерживающий YAML и Java Time.
     */
    @Bean
    public ObjectMapper yamlObjectMapper() {
        var mapper = new ObjectMapper(new YAMLFactory());
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }

    /**
     * JSON ObjectMapper для сериализации правил в БД.
     * Поддерживает полиморфные Condition и Action.
     */
    @Bean
    public ObjectMapper jsonObjectMapper() {
        var mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.registerModule(new JacksonConditionModule());
        mapper.registerModule(new JacksonActionModule());
        return mapper;
    }

    @Bean
    public CacheManager cacheManager() {
        return new CaffeineCacheManager("slaProfiles", "holidays");
    }
}
