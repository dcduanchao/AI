package com.dc.ai.dto;

import java.time.Instant;

public record LoginResponseDto(
        String token,
        String tokenType,
        Instant expiresAt,
        UserInfoDto user
) {
}
