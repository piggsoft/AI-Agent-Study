package com.piggsoft.webchat.service.springai;

import com.piggsoft.webchat.dto.ChatRequest;
import org.springframework.ai.chat.model.ChatResponse;
import reactor.core.publisher.Flux;

public interface SpringAiChatService {

    Flux<ChatResponse> chatStream(ChatRequest request);
}
