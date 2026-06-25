package com.dc.ai.controller;

import com.dc.ai.dto.ModelsResponseDto;
import com.dc.ai.service.ModelRegistry;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/models")
public class ModelController {

    private final ModelRegistry modelRegistry;

    public ModelController(ModelRegistry modelRegistry) {
        this.modelRegistry = modelRegistry;
    }

    @GetMapping
    public ModelsResponseDto models() {
        return modelRegistry.getSnapshot();
    }
}
