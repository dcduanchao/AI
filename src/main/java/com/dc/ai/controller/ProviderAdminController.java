package com.dc.ai.controller;

import com.dc.ai.dto.SyncResultDto;
import com.dc.ai.service.ModelSyncService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/admin/providers")
public class ProviderAdminController {

    private final ModelSyncService modelSyncService;

    public ProviderAdminController(ModelSyncService modelSyncService) {
        this.modelSyncService = modelSyncService;
    }

    @PostMapping("/{aiCode}/sync-models")
    public Mono<SyncResultDto> syncModels(@PathVariable String aiCode) {
        return modelSyncService.syncProvider(aiCode);
    }

    @PostMapping("/sync-all-models")
    public Flux<SyncResultDto> syncAllModels() {
        return modelSyncService.syncAllEnabledProviders();
    }
}
