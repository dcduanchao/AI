package com.dc.ai.service;

import com.dc.ai.config.JwtProperties;
import com.dc.ai.domain.UserEntity;
import com.dc.ai.dto.LoginRequestDto;
import com.dc.ai.dto.LoginResponseDto;
import com.dc.ai.dto.UserInfoDto;
import com.dc.ai.mapper.UserMapper;
import com.dc.ai.util.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Instant;

@Service
public class AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;

    public AuthService(UserMapper userMapper,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil,
                       JwtProperties jwtProperties) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.jwtProperties = jwtProperties;
    }

    public Mono<LoginResponseDto> login(LoginRequestDto request) {
        return Mono.fromCallable(() -> {
                    if (request == null || request.getUsername() == null || request.getPassword() == null) {
                        throw new IllegalArgumentException("用户名或密码错误");
                    }
                    UserEntity user = userMapper.selectByUsername(request.getUsername());
                    // 不区分「用户不存在」与「密码错误」，防用户名枚举
                    if (user == null || !user.isEnabled()
                            || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                        throw new IllegalArgumentException("用户名或密码错误");
                    }
                    String token = jwtUtil.generateToken(user.getId(), user.getUsername());
                    Instant expiresAt = Instant.now().plusSeconds(jwtProperties.getExpireMinutes() * 60L);
                    return new LoginResponseDto(
                            token,
                            jwtProperties.getPrefix(),
                            expiresAt,
                            new UserInfoDto(user.getId(), user.getUsername(), user.getNickname()));
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<UserInfoDto> me(Long userId) {
        return Mono.fromCallable(() -> {
                    UserEntity user = userMapper.selectById(userId);
                    if (user == null) {
                        throw new IllegalArgumentException("用户不存在");
                    }
                    return new UserInfoDto(user.getId(), user.getUsername(), user.getNickname());
                })
                .subscribeOn(Schedulers.boundedElastic());
    }
}
