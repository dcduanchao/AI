package com.dc.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequestDto {

    private String aiCode;
    private String model;
    private Long conversationId;
    private List<ChatMessageDto> messages;
    private Double temperature;
}
