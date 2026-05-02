package com.piggsoft.webchat.service.custom;

import com.piggsoft.webchat.dto.ChatRequest;
import reactor.core.publisher.Flux;

public interface CustomToolCallService {

    Flux<String> chatStream(ChatRequest request);
}
