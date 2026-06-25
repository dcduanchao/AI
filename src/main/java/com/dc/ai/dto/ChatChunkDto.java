package com.dc.ai.dto;

public record ChatChunkDto(
        String aiCode,
        String model,
        String content,
        boolean done
) {
}
