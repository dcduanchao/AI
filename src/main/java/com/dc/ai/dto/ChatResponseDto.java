package com.dc.ai.dto;

public record ChatResponseDto(
        String aiCode,
        String model,
        String content
) {
}
