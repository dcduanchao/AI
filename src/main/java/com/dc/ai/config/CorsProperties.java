package com.dc.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * CORS 配置，绑定 application.properties 中的 app.cors.* 项。
 */
@ConfigurationProperties(prefix = "app.cors")
public record CorsProperties(
        Boolean allowAll,
        List<String> allowedOrigins,
        List<String> allowedMethods,
        List<String> allowedHeaders,
        List<String> exposedHeaders,
        Boolean allowCredentials,
        Long maxAge
) {
    public CorsProperties {
        allowAll = allowAll != null ? allowAll : false;
        allowedOrigins = allowedOrigins != null ? allowedOrigins : List.of();
        allowedMethods = allowedMethods != null ? allowedMethods : List.of("GET", "POST", "PUT", "DELETE", "OPTIONS");
        allowedHeaders = allowedHeaders != null ? allowedHeaders : List.of("*");
        exposedHeaders = exposedHeaders != null ? exposedHeaders : List.of();
        allowCredentials = allowCredentials != null ? allowCredentials : true;
        maxAge = maxAge != null ? maxAge : 3600L;
    }
}
