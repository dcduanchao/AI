package com.dc.ai.config;

import com.alibaba.fastjson2.JSONObject;
import com.dc.ai.util.JwtUtil;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * 全局 JWT 鉴权过滤器（WebFlux 响应式）。
 * 白名单直接放行；其余请求要求 Authorization: Bearer <token>，校验通过后把 userId 写入 exchange attribute。
 */
@Component
public class JwtAuthWebFilter implements WebFilter, Ordered {

    /** 无需登录即可访问的路径。 */
    private static final List<String> WHITELIST = List.of(
            "/api/auth/login"
    );

    private final JwtUtil jwtUtil;
    private final JwtProperties properties;

    public JwtAuthWebFilter(JwtUtil jwtUtil, JwtProperties properties) {
        this.jwtUtil = jwtUtil;
        this.properties = properties;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        // CORS 预检放行
        if (HttpMethod.OPTIONS.equals(exchange.getRequest().getMethod())) {
            return chain.filter(exchange);
        }

        String path = exchange.getRequest().getPath().value();
        if (isWhitelisted(path)) {
            return chain.filter(exchange);
        }

        String header = exchange.getRequest().getHeaders().getFirst(properties.header());
        String tokenPrefix = properties.prefix() + " ";
        if (header == null || !header.startsWith(tokenPrefix)) {
            return unauthorized(exchange, "未登录");
        }

        String token = header.substring(tokenPrefix.length()).trim();
        if (!jwtUtil.validate(token)) {
            return unauthorized(exchange, "登录已过期或无效");
        }

        Long userId = jwtUtil.parseUserId(token);
        exchange.getAttributes().put("userId", userId);
        return chain.filter(exchange);
    }

    private boolean isWhitelisted(String path) {
        return WHITELIST.contains(path);
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] bytes = JSONObject.toJSONString(Map.of("message", message))
                .getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        // 尽量靠前执行
        return -100;
    }
}
