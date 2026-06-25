package com.dc.ai.dto;

import java.util.List;

public record ChatRequestDto(
        String aiCode,
        String model,
        Long conversationId,
        List<ChatMessageDto> messages,
        Double temperature
) {
}
