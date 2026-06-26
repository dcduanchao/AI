package com.dc.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * CORS 配置，绑定 application.properties 中的 app.cors.* 项。
 */
@Data
@ConfigurationProperties(prefix = "app.cors")
public class CorsProperties {

    private boolean allowAll = false;
    private List<String> allowedOrigins = List.of();
    private List<String> allowedMethods = List.of("GET", "POST", "PUT", "DELETE", "OPTIONS");
    private List<String> allowedHeaders = List.of("*");
    private List<String> exposedHeaders = List.of();
    private boolean allowCredentials = true;
    private long maxAge = 3600L;
}
