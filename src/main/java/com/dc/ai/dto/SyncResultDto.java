package com.dc.ai.dto;

public record SyncResultDto(
        Long providerId,
        String aiCode,
        int modelCount,
        String status,
        String message
) {
}
