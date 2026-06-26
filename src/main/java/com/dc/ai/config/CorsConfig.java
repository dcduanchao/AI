package com.dc.ai.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

/**
 * WebFlux CORS 配置。
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsWebFilter corsWebFilter(CorsProperties properties) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowCredentials(properties.isAllowCredentials());
        configuration.setAllowedMethods(properties.getAllowedMethods());
        configuration.setAllowedHeaders(properties.getAllowedHeaders());
        configuration.setExposedHeaders(properties.getExposedHeaders());
        configuration.setMaxAge(properties.getMaxAge());

        if (properties.isAllowAll()) {
            configuration.addAllowedOriginPattern("*");
        } else {
            properties.getAllowedOrigins().forEach(configuration::addAllowedOrigin);
        }

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return new CorsWebFilter(source);
    }
}
