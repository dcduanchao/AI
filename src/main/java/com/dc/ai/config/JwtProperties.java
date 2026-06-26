package com.dc.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT 配置，绑定 application.properties 中的 jwt.* 项。
 * 项目已开启 @ConfigurationPropertiesScan，无需额外注册。
 */
@Data
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    private String secret;
    private long expireMinutes;
    private String header;
    private String prefix;
}
