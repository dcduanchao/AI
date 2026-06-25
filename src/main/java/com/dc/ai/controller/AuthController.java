package com.dc.ai.controller;

import com.dc.ai.dto.LoginRequestDto;
import com.dc.ai.dto.LoginResponseDto;
import com.dc.ai.dto.UserInfoDto;
import com.dc.ai.service.AuthService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public Mono<LoginResponseDto> login(@RequestBody LoginRequestDto request) {
        return authService.login(request);
    }

    /** userId 由 JwtAuthWebFilter 校验通过后写入 exchange attribute。 */
    @GetMapping("/me")
    public Mono<UserInfoDto> me(@RequestAttribute("userId") Long userId) {
        return authService.me(userId);
    }
}
