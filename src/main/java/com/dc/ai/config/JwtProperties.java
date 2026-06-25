package com.dc.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT 配置，绑定 application.properties 中的 jwt.* 项。
 * 项目已开启 @ConfigurationPropertiesScan，无需额外注册。
 */
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        String secret,
        long expireMinutes,
        String header,
        String prefix
) {
}
