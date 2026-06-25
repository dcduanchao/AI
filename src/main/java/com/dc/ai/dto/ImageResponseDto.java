package com.dc.ai.dto;

import java.util.List;

public record ImageResponseDto(
        String aiCode,
        String model,
        String status,
        List<String> urls,
        String errorMessage
) {
}
