package com.dc.ai.service;

import com.dc.ai.domain.ProviderEntity;
import com.dc.ai.dto.ImageRequestDto;
import com.dc.ai.dto.ImageResponseDto;
import com.dc.ai.provider.AiProviderClient;
import com.dc.ai.provider.ProviderClientRouter;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class ImageService {

    private final ModelRegistry modelRegistry;
    private final ProviderClientRouter providerClientRouter;

    public ImageService(ModelRegistry modelRegistry, ProviderClientRouter providerClientRouter) {
        this.modelRegistry = modelRegistry;
        this.providerClientRouter = providerClientRouter;
    }

    public Mono<ImageResponseDto> generate(ImageRequestDto request) {
        ProviderEntity provider = modelRegistry.findProvider(request.getAiCode())
                .orElseThrow(() -> new IllegalArgumentException("Provider not found in registry: " + request.getAiCode()));
        if (!modelRegistry.hasModel(request.getAiCode(), request.getModel())) {
            throw new IllegalArgumentException("Model not found in registry for provider " + request.getAiCode() + ": " + request.getModel());
        }
        AiProviderClient client = providerClientRouter.get(provider.getAdapterType());
        return client.generateImage(provider, request.getModel(), request);
    }
}
