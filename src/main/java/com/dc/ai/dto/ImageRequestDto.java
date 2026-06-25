package com.dc.ai.dto;

public record ImageRequestDto(
        String aiCode,
        String model,
        String prompt,
        String size,
        Integer count
) {
}
