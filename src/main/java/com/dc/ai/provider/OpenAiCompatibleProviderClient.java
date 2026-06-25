package com.dc.ai.provider;

import com.alibaba.fastjson2.JSONObject;
import com.dc.ai.domain.AdapterType;
import com.dc.ai.domain.ProviderEntity;
import com.dc.ai.dto.ChatChunkDto;
import com.dc.ai.dto.ChatMessageDto;
import com.dc.ai.dto.ChatRequestDto;
import com.dc.ai.dto.ChatResponseDto;
import com.dc.ai.dto.ImageRequestDto;
import com.dc.ai.dto.ImageResponseDto;
import com.dc.ai.dto.ModelDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;

@Component
@Slf4j
public class OpenAiCompatibleProviderClient implements AiProviderClient {

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    public OpenAiCompatibleProviderClient(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        this.webClientBuilder = webClientBuilder;
        this.objectMapper = objectMapper;
    }

    @Override
    public AdapterType adapterType() {
        return AdapterType.OPENAI_COMPATIBLE;
    }

    @Override
    public Mono<List<ModelDto>> fetchModels(ProviderEntity provider) {
        Mono<List<ModelDto>> map = webClient(provider)
                .get()
                .uri("/models")
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(root -> {
                    List<ModelDto> models = new ArrayList<>();
                    JsonNode data = root.path("data");
                    if (data.isArray()) {
                        for (JsonNode item : data) {
                            String modelCode = item.path("id").asText();
                            if (!modelCode.isBlank()) {
                                models.add(new ModelDto(modelCode));
                            }
                        }
                    }
                    log.info("models: {},{}", provider.getCode(), models);
                    return models;
                });
//        log.info("models: {},{}", provider.getCode(), map.block());
        return map;
    }

    @Override
    public Mono<ChatResponseDto> chat(ProviderEntity provider, String model, ChatRequestDto request) {
        return webClient(provider)
                .post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(chatBody(model, request, false))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(root -> new ChatResponseDto(
                        provider.getCode(),
                        model,
                        root.path("choices").path(0).path("message").path("content").asText()
                ));
    }

    @Override
    public Flux<ChatChunkDto> streamChat(ProviderEntity provider, String model, ChatRequestDto request) {
        return webClient(provider)
                .post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue(chatBody(model, request, true))
                .retrieve()
                .bodyToFlux(String.class)
                .map(this::parseStreamContent)
                .filter(Objects::nonNull)
                .filter(s -> !s.isBlank())
                .map(content -> {
                    if("[DONE]".equals(content)) {
                        return new ChatChunkDto(provider.getCode(), model, content, true);
                    }else {

                       return new ChatChunkDto(provider.getCode(), model, content, false);
                    }
                }).onErrorResume(Exception.class,e->{
                    log.error("streamChat error={}",e);
                    return Mono.just(new ChatChunkDto(provider.getCode(), model, "ERROR", true));
                });
    }

    @Override
    public Mono<ImageResponseDto> generateImage(ProviderEntity provider, String model, ImageRequestDto request) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("prompt", request.prompt());
        body.put("size", request.size() == null ? "1024x1024" : request.size());
        body.put("n", request.count() == null ? 1 : request.count());
        log.info("generateImage body: {}", body);
        return webClient(provider)
                .post()
                .uri("/images/generations")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(root -> {
                    log.info("image: {},={}", provider.getCode(), root);
                    List<String> urls = new ArrayList<>();
                    JsonNode data = root.path("data");
                    if (data.isArray()) {
                        for (JsonNode item : data) {
                            String url = item.path("url").asText();
                            if (!url.isBlank()) {
                                urls.add(url);
                            }
                        }
                    }
                    return new ImageResponseDto(provider.getCode(), model, "success", urls, null);
                }).onErrorResume(WebClientResponseException.class, e -> {
                    log.error("image api failed", e);
                    return Mono.just(new ImageResponseDto(null, model,"IMAGE_SERVICE_ERROR", new ArrayList<>(),e.getMessage()));
                });
    }

    private WebClient webClient(ProviderEntity provider) {
        return webClientBuilder
                .baseUrl(versionedBaseUrl(provider.getBaseUrl()))
                .defaultHeaders(headers -> headers.setBearerAuth(provider.getApiKey()))
                .build();
    }

    private Map<String, Object> chatBody(String modelCode, ChatRequestDto request, boolean stream) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", modelCode);
        body.put("stream", stream);
        if (request.temperature() != null) {
            body.put("temperature", request.temperature());
        }
        body.put("messages", request.messages().stream().map(this::messageBody).toList());
        log.info("chatBody={}", JSONObject.toJSONString(body));
        return body;
    }

    private Map<String, String> messageBody(ChatMessageDto message) {
        return Map.of(
                "role", message.role(),
                "content", message.content()
        );
    }

    private String parseStreamContent(String line) {
        log.info("parseStreamContent={}", line);
        if(line == null || line.isBlank()){
            return "";
        }
        if ( "[DONE]".equals(line)) {
            return "[DONE]";
        }

        String json = line.startsWith("data:") ? line.substring(5).trim() : line.trim();
        if ("[DONE]".equals(json)) {
            return "[DONE]";
        }

        try {
            JsonNode root = objectMapper.readTree(json);
            return root.path("choices").path(0).path("delta").path("content").asText();
        } catch (Exception ignored) {
            return json;
        }
    }

    private String trimTrailingSlash(String value) {
        if (value.endsWith("/")) {
            return value.substring(0, value.length() - 1);
        }
        return value;
    }

    private String versionedBaseUrl(String value) {
        String baseUrl = trimTrailingSlash(value);
        if (baseUrl.endsWith("/v1")) {
            return baseUrl;
        }
        return baseUrl + "/v1";
    }
}
