package com.dc.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImageResponseDto {

    private String aiCode;
    private String model;
    private String status;
    private List<String> urls;
    private String errorMessage;
}
