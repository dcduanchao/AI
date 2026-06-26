package com.dc.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatChunkDto {

    private String aiCode;
    private String model;
    private String content;
    private boolean done;
}
