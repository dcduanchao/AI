package com.dc.ai.controller;

import com.dc.ai.dto.ChatChunkDto;
import com.dc.ai.dto.ChatRequestDto;
import com.dc.ai.dto.ChatResponseDto;
import com.dc.ai.service.ChatService;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping
    public Mono<ChatResponseDto> chat(@RequestBody ChatRequestDto request) {
        return chatService.chat(request);
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<ChatChunkDto>> streamChat(@RequestBody ChatRequestDto request) {
        return chatService.streamChat(request)
                .map(chunk -> ServerSentEvent.<ChatChunkDto>builder(chunk)
                        .event(chunk.isDone() ? "done" : "chunk")
                        .build());
    }
}
