package com.dc.ai.util;

import com.dc.ai.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 生成 / 解析 / 校验。算法 HS256，payload 只放 userId(subject) 与 username，不放敏感信息、不放角色。
 */
@Component
public class JwtUtil {

    private final JwtProperties properties;
    private final SecretKey key;

    public JwtUtil(JwtProperties properties) {
        this.properties = properties;
        this.key = Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(Long userId, String username) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + properties.getExpireMinutes() * 60_000L);
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(key)
                .compact();
    }

    public Long parseUserId(String token) {
        return Long.valueOf(parseClaims(token).getSubject());
    }

    public String parseUsername(String token) {
        return parseClaims(token).get("username", String.class);
    }

    /** 静默校验：验签 + 验过期，失败返回 false。 */
    public boolean validate(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
