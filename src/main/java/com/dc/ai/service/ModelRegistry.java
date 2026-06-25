package com.dc.ai.service;

import com.dc.ai.domain.ProviderEntity;
import com.dc.ai.dto.ModelsResponseDto;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class ModelRegistry {

    private final AtomicReference<Map<String, ProviderEntity>> providersByAiCode = new AtomicReference<>(Map.of());
    private final AtomicReference<Map<String, List<String>>> modelsByAiCode = new AtomicReference<>(Map.of());
    private final AtomicReference<ModelsResponseDto> snapshot = new AtomicReference<>(new ModelsResponseDto(Map.of()));

    public ModelsResponseDto getSnapshot() {
        return snapshot.get();
    }

    public Optional<ProviderEntity> findProvider(String aiCode) {
        if (aiCode == null || aiCode.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(providersByAiCode.get().get(aiCode));
    }

    public boolean hasModel(String aiCode, String model) {
        if (aiCode == null || aiCode.isBlank() || model == null || model.isBlank()) {
            return false;
        }
        return modelsByAiCode.get().getOrDefault(aiCode, List.of()).contains(model);
    }

    public void refreshProviders(List<ProviderEntity> providers) {
        Map<String, ProviderEntity> nextProviders = new LinkedHashMap<>();
        Map<String, List<String>> nextModels = new LinkedHashMap<>(modelsByAiCode.get());

        for (ProviderEntity provider : providers) {
            nextProviders.put(provider.getCode(), provider);
            nextModels.putIfAbsent(provider.getCode(), List.of());
        }
        //保留次重复
        nextModels.keySet().retainAll(nextProviders.keySet());
        providersByAiCode.set(Map.copyOf(nextProviders));
        modelsByAiCode.set(copyModels(nextModels));
        rebuildSnapshot();
    }

    public void refreshModels(String aiCode, List<String> models) {
        Map<String, List<String>> nextModels = new LinkedHashMap<>(modelsByAiCode.get());
        nextModels.put(aiCode, List.copyOf(models));
        modelsByAiCode.set(copyModels(nextModels));
        rebuildSnapshot();
    }

    private void rebuildSnapshot() {
        snapshot.set(new ModelsResponseDto(modelsByAiCode.get()));
    }

    private Map<String, List<String>> copyModels(Map<String, List<String>> models) {
        Map<String, List<String>> copy = new LinkedHashMap<>();
        models.forEach((aiCode, modelIds) -> copy.put(aiCode, List.copyOf(modelIds)));
        return Map.copyOf(copy);
    }
}
