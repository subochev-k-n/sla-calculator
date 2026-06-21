package io.itsm.sla.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.concurrent.TimeUnit;

/**
 * Конфигурация веб-слоя.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // Корень → новая страница (чтобы старый index.html в кэше не мешал)
        registry.addViewController("/").setViewName("forward:/sla-calc.html");
        registry.addViewController("/index.html").setViewName("forward:/sla-calc.html");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/sla-calc.html", "/", "/*.html")
            .addResourceLocations("classpath:/static/")
            .setCacheControl(CacheControl.noCache().cachePrivate()
                .mustRevalidate().staleIfError(0, TimeUnit.SECONDS));
    }
}
