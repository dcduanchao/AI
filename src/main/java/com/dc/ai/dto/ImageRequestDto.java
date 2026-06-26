package com.dc.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImageRequestDto {

    private String aiCode;
    private String model;
    private String prompt;
    private String size;
    private Integer count;
    private Integer failCount;
}
