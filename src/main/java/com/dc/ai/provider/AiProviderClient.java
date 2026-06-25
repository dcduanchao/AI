package com.dc.ai.provider;

import com.dc.ai.domain.AdapterType;
import com.dc.ai.domain.ProviderEntity;
import com.dc.ai.dto.ChatChunkDto;
import com.dc.ai.dto.ChatRequestDto;
import com.dc.ai.dto.ChatResponseDto;
import com.dc.ai.dto.ImageRequestDto;
import com.dc.ai.dto.ImageResponseDto;
import com.dc.ai.dto.ModelDto;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface AiProviderClient {

    AdapterType adapterType();

    Mono<List<ModelDto>> fetchModels(ProviderEntity provider);

    Mono<ChatResponseDto> chat(ProviderEntity provider, String model, ChatRequestDto request);

    Flux<ChatChunkDto> streamChat(ProviderEntity provider, String model, ChatRequestDto request);

    Mono<ImageResponseDto> generateImage(ProviderEntity provider, String model, ImageRequestDto request);
}
