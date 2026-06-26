package com.dc.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponseDto {

    private String token;
    private String tokenType;
    private Instant expiresAt;
    private UserInfoDto user;
}
