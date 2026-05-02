package com.piggsoft.webchat.controller;

import com.piggsoft.webchat.dto.ApiResponse;
import com.piggsoft.webchat.dto.ChatHistoryDTO;
import com.piggsoft.webchat.dto.ChatRequest;
import com.piggsoft.webchat.entity.ChatHistory;
import com.piggsoft.webchat.mapper.ChatHistoryMapper;
import com.piggsoft.webchat.service.ChatService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AbstractMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final ChatService chatService;
    private final ChatHistoryMapper chatHistoryMapper;
    private final ObjectMapper objectMapper;


    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamChat(@RequestBody ChatRequest request) {
        Prompt prompt = new Prompt(new UserMessage(request.message()));
        return chatService.chatStream(request)
                // 使用 map + filter 组合替代复杂的三元运算符
                .map(ChatResponse::getResult)
                .filter(Objects::nonNull)
                .map(result -> Optional.ofNullable(result.getOutput().getText()).orElse(""))
                // 再次过滤确保发给前端的不是空串
                .filter(StringUtils::hasLength);
    }

    @GetMapping("/history/{sessionId}")
    public ApiResponse<List<ChatHistoryDTO>> getHistory(@PathVariable String sessionId) {
        List<ChatHistory> histories = chatHistoryMapper.selectBySessionId(sessionId);

        List<ChatHistoryDTO> dtos = histories.stream()
                .map(this::toDTO)
                .toList();

        return ApiResponse.success(dtos);
    }

    private ChatHistoryDTO toDTO(ChatHistory history) {
        Object queryConditions = null;
        if (history.getQueryConditions() != null) {
            try {
                queryConditions = objectMapper.readValue(history.getQueryConditions(), Object.class);
            } catch (JsonProcessingException e) {
                queryConditions = history.getQueryConditions();
            }
        }

        return new ChatHistoryDTO(
            history.getId(),
            history.getSessionId(),
            history.getRole(),
            history.getContent(),
            queryConditions,
            history.getCreatedAt()
        );
    }
}
