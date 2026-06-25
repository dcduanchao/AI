package com.dc.ai;

import com.dc.ai.service.ModelSyncService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class ModelRegistryLoader implements ApplicationRunner {

    private final ModelSyncService modelSyncService;

    public ModelRegistryLoader(ModelSyncService modelSyncService) {
        this.modelSyncService = modelSyncService;
    }

    @Override
    public void run(ApplicationArguments args) {
        modelSyncService.syncAllEnabledProviders().subscribe();
    }
}
