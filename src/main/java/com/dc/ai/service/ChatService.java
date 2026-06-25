package com.dc.ai.service;

import com.dc.ai.domain.ProviderEntity;
import com.dc.ai.dto.ChatChunkDto;
import com.dc.ai.dto.ChatRequestDto;
import com.dc.ai.dto.ChatResponseDto;
import com.dc.ai.provider.AiProviderClient;
import com.dc.ai.provider.ProviderClientRouter;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class ChatService {

    private final ModelRegistry modelRegistry;
    private final ProviderClientRouter providerClientRouter;

    public ChatService(ModelRegistry modelRegistry, ProviderClientRouter providerClientRouter) {
        this.modelRegistry = modelRegistry;
        this.providerClientRouter = providerClientRouter;
    }

    public Mono<ChatResponseDto> chat(ChatRequestDto request) {
        ProviderEntity provider = getProvider(request.aiCode(), request.model());
        AiProviderClient client = providerClientRouter.get(provider.getAdapterType());
        return client.chat(provider, request.model(), request);
    }

    public Flux<ChatChunkDto> streamChat(ChatRequestDto request) {
        ProviderEntity provider = getProvider(request.aiCode(), request.model());
        AiProviderClient client = providerClientRouter.get(provider.getAdapterType());
        return client.streamChat(provider, request.model(), request);
    }

    private ProviderEntity getProvider(String aiCode, String model) {
        ProviderEntity provider = modelRegistry.findProvider(aiCode)
                .orElseThrow(() -> new IllegalArgumentException("Provider not found in registry: " + aiCode));
        if (!modelRegistry.hasModel(aiCode, model)) {
            throw new IllegalArgumentException("Model not found in registry for provider " + aiCode + ": " + model);
        }
        return provider;
    }
}
