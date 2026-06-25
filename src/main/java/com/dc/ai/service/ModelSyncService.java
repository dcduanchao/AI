package com.dc.ai.service;

import com.alibaba.fastjson2.JSONObject;
import com.dc.ai.domain.ProviderEntity;
import com.dc.ai.domain.ProviderSyncLogEntity;
import com.dc.ai.dto.ModelDto;
import com.dc.ai.dto.SyncResultDto;
import com.dc.ai.mapper.ProviderMapper;
import com.dc.ai.mapper.ProviderSyncLogMapper;
import com.dc.ai.provider.AiProviderClient;
import com.dc.ai.provider.ProviderClientRouter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

@Slf4j
@Service
public class ModelSyncService {

    private final ProviderMapper providerMapper;
    private final ProviderSyncLogMapper syncLogMapper;
    private final ProviderClientRouter providerClientRouter;
    private final ModelRegistry modelRegistry;

    public ModelSyncService(ProviderMapper providerMapper,
                            ProviderSyncLogMapper syncLogMapper,
                            ProviderClientRouter providerClientRouter,
                            ModelRegistry modelRegistry) {
        this.providerMapper = providerMapper;
        this.syncLogMapper = syncLogMapper;
        this.providerClientRouter = providerClientRouter;
        this.modelRegistry = modelRegistry;
    }

    public List<ProviderEntity> refreshEnabledProvidersFromDatabase() {
        List<ProviderEntity> providers = providerMapper.selectEnabledOrderByNameAsc();
        log.info("refreshEnabledProvidersFromDatabase: providers = {}", JSONObject.toJSONString(providers));
        modelRegistry.refreshProviders(providers);
        return providers;
    }

    public Flux<SyncResultDto> syncAllEnabledProviders() {
        return Flux.defer(() -> Flux.fromIterable(refreshEnabledProvidersFromDatabase()))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(this::syncProvider);
    }

    public Mono<SyncResultDto> syncProvider(String aiCode) {
        return Mono.fromCallable(() -> {
                    ProviderEntity provider = providerMapper.selectEnabledByAiCode(aiCode);
                    if (provider == null) {
                        throw new IllegalArgumentException("Enabled provider not found: " + aiCode);
                    }
                    modelRegistry.refreshProviders(providerMapper.selectEnabledOrderByNameAsc());
                    return provider;
                })
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(this::syncProvider);
    }

    private Mono<SyncResultDto> syncProvider(ProviderEntity provider) {
        AiProviderClient client = providerClientRouter.get(provider.getAdapterType());
        return client.fetchModels(provider)
                .publishOn(Schedulers.boundedElastic())
                .map(remoteModels -> saveSyncedModels(provider, remoteModels))
                .onErrorResume(error -> Mono.<SyncResultDto>fromCallable(() -> saveSyncError(provider, error))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    public SyncResultDto saveSyncedModels(ProviderEntity provider, List<ModelDto> remoteModels) {
        log.info("saveSyncedModels ={},{}",provider.getCode(), JSONObject.toJSONString(remoteModels));
        List<String> modelIds = remoteModels.stream()
                .map(ModelDto::id)
                .filter(id -> id != null && !id.isBlank())
                .distinct()
                .toList();
        modelRegistry.refreshModels(provider.getCode(), modelIds);
        syncLogMapper.insert(new ProviderSyncLogEntity(provider, "success", "Loaded " + modelIds.size() + " models"));
        return new SyncResultDto(provider.getId(), provider.getCode(), modelIds.size(), "success", null);
    }

    public SyncResultDto saveSyncError(ProviderEntity provider, Throwable error) {
        modelRegistry.refreshModels(provider.getCode(), List.of());
        syncLogMapper.insert(new ProviderSyncLogEntity(provider, "failed", error.getMessage()));
        return new SyncResultDto(provider.getId(), provider.getCode(), 0, "failed", error.getMessage());
    }
}
