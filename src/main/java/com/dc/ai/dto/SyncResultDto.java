package com.dc.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SyncResultDto {

    private Long providerId;
    private String aiCode;
    private int modelCount;
    private String status;
    private String message;
}
