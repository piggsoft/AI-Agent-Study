package com.piggsoft.webchat.service;

import com.piggsoft.webchat.dto.ChatRequest;
import org.springframework.ai.chat.model.ChatResponse;
import reactor.core.publisher.Flux;

public interface ChatService {

    Flux<ChatResponse> chatStream(ChatRequest request);
}
