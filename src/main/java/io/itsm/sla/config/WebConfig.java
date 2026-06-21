package io.itsm.sla.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.concurrent.TimeUnit;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/").setViewName("forward:/calc.html");
        registry.addViewController("/index.html").setViewName("forward:/calc.html");
        registry.addViewController("/sla-calc.html").setViewName("forward:/calc.html");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/calc.html", "/", "/*.html")
            .addResourceLocations("classpath:/static/")
            .setCacheControl(CacheControl.noCache().cachePrivate()
                .mustRevalidate().staleIfError(0, TimeUnit.SECONDS));
    }
}
