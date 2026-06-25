package com.dc.ai.dto;

import java.util.List;
import java.util.Map;

public record ModelsResponseDto(
        Map<String, List<String>> providers
) {
}
